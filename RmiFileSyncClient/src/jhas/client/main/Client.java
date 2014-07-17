package jhas.client.main;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jhas.client.FileSyncManager;

public class Client {
	public static final String LOCAL_DIR_PATH = "C:\\Users\\Jonathan\\Documents";
	public static void main(String[] args) {
		ExecutorService executor = null;
		try{
			executor = Executors.newCachedThreadPool();
			FileSyncManager manager = new FileSyncManager(new File(LOCAL_DIR_PATH), executor);
			executor.execute(manager);
		}catch(RuntimeException e){
			System.out.printf("Quebrou: %s\n", e.getMessage());
		}
	}
}
