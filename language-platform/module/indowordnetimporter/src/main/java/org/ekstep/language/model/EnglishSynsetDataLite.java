package org.ekstep.language.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="tbl_all_english_synset_data")
public class EnglishSynsetDataLite implements LanguageSynsetDataLite{
	
	@Id
	private int synset_id;
	
	@Column(name = "synset", unique = false, nullable = false, length = 100000)
	private byte[] synset;
	
	public EnglishSynsetDataLite() {
		super();
	}
	
	public EnglishSynsetDataLite(int synset_id, byte[] synset) {
		super();
		this.synset_id = synset_id;
		this.synset = synset;
	}

	public int getSynset_id() {
		return synset_id;
	}
	public void setSynset_id(int synset_id) {
		this.synset_id = synset_id;
	}
	public byte[] getSynset() {
		return synset;
	}
	public void setSynset(byte[] synset) {
		this.synset = synset;
	}
	
	public SynsetDataLite getSynsetDataLite(){
		SynsetDataLite synsetDataLite = new SynsetDataLite();
		synsetDataLite.setSynset_id(this.synset_id);
		synsetDataLite.setSynset(this.synset);
		return synsetDataLite;
	}
}
