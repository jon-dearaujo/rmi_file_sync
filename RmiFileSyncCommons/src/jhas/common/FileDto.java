package jhas.common;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

public class FileDto implements Serializable{
	private static final long serialVersionUID = 1L;
	private String name;
	private Date lastModificationDate;
	private String path;
	private String parentName;
	
	public FileDto() {}
	
	public FileDto(String name, Date lastModDate, String path){
		this.name = name;
		lastModificationDate = lastModDate;
		this.path = path;
		File file = new File(path);
		parentName = file.toPath().getParent().getFileName().toString();
		
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getLastModificationDate() {
		return lastModificationDate;
	}
	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentname) {
		this.parentName = parentname;
	}
}
