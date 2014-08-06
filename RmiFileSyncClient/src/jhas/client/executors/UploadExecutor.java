package jhas.client.executors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import jhas.common.FileDto;
import jhas.common.FileSync;

public class UploadExecutor implements Runnable{
	private List<FileDto> toUpload;
	private FileSync fileSync;
	
	public UploadExecutor(List<FileDto> list, FileSync fileSync) {
		toUpload = list;
		this.fileSync = fileSync;
	}
	@Override
	public void run() {
		if(!toUpload.isEmpty()){
			for(FileDto file : toUpload){
				try {
					fileSync.update(Files.readAllBytes(new File(file.getPath()).toPath()), file);
					System.out.printf("%s - %s uploaded\n", Thread.currentThread().getName(), file.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Upload is done!");
	}
}
