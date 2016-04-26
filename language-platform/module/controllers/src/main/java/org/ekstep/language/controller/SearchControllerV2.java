package org.ekstep.language.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ekstep.language.common.enums.LanguageObjectTypes;
import org.ekstep.language.mgr.IDictionaryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ilimi.common.controller.BaseController;
import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;

@Controller
@RequestMapping("v2/language/dictionary/search")
public class SearchControllerV2 extends BaseController {

    @Autowired
    private IDictionaryManager dictionaryManager;

    private static Logger LOGGER = LogManager.getLogger(ParserController.class.getName());

    @RequestMapping(value = "/{languageId}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> create(@PathVariable(value = "languageId") String languageId,
            @RequestBody Map<String, Object> map, @RequestHeader(value = "user-id") String userId) {
        String apiId = "word.search";
        Request request = getRequest(map);
        try {
            Response response = dictionaryManager.listV2(languageId, LanguageObjectTypes.Word.name(), request);
            LOGGER.info("Search | Response: " + response);
            return getResponseEntity(response, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        } catch (Exception e) {
            LOGGER.error("Search | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId,
                    (null != request.getParams()) ? request.getParams().getMsgid() : null);
        }
    }

}
