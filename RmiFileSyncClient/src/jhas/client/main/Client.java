package jhas.client.main;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jhas.client.FileSyncManager;

public class Client {
	public static final String LOCAL_DIR_PATH = "C:\\Users\\Jonathan\\Documents";
	public static void main(String[] args) {
		String dirPath = "";
		System.out.println("Digite o diretório para sincronizar:");
		Scanner scan = new Scanner(System.in);
		dirPath = scan.nextLine();
		scan.close();
		ExecutorService executor = null;
		try{
			File dir = new File(dirPath);
			if(dir.exists() && dir.isDirectory()){
				executor = Executors.newCachedThreadPool();
				FileSyncManager manager = new FileSyncManager(dir, executor);
				executor.execute(manager);
			}else{
				System.out.println("Diretório inválido. Roda denovo!");
			}
		}catch(RuntimeException e){
			System.out.printf("Quebrou: %s\n", e.getMessage());
		}
	}
}
