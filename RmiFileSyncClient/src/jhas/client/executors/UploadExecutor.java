package jhas.client.executors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import jhas.common.FileSync;

public class UploadExecutor implements Runnable{
	private List<File> toUpload;
	private FileSync fileSync;
	
	public UploadExecutor(List<File> list, FileSync fileSync) {
		toUpload = list;
		this.fileSync = fileSync;
	}
	@Override
	public void run() {
		if(!toUpload.isEmpty()){
			for(File file : toUpload){
				try {
					fileSync.update(Files.readAllBytes(file.toPath()), file);
					System.out.printf("%s - %s uploaded\n", Thread.currentThread().getName(), file.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Upload is done!");
	}
}
