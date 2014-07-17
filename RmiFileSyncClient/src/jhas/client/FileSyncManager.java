package jhas.client;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import jhas.client.executors.DownloadExecutor;
import jhas.client.executors.UploadExecutor;
import jhas.common.Constants;
import jhas.common.FileSync;

public class FileSyncManager implements Runnable{
	private File localDir;
	private File serverDir;
	private FileSync fileSync;
	private List<File> toUpload;
	private List<File> toDownload;
	private ExecutorService executor;
	
	public FileSyncManager(File localDir, ExecutorService executor) {
		this.localDir = localDir;
		this.executor = executor;
		toUpload = new ArrayList<>();
		toDownload = new ArrayList<>();
		try {
			this.fileSync = (FileSync) LocateRegistry.getRegistry(Constants.SERVER_RMI_IP, Constants.SERVER_RMI_PORT)
				.lookup(Constants.SERVER_RMI_OBJECT_ID);
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public void calculateDifferences(){
		try{
			if(localDir.isDirectory()){
				String dirName = localDir.getName();
				serverDir = fileSync.getDirectoryDescription(dirName);
				if(serverDir.exists() && serverDir.isDirectory()){
					findFilesExistingOnServerButNotOnLocal();
					findFilesExistingOnBothButClientUpToDate();
					findFilesExistingOnBothButServerUpToDate();
				}
				findFilesExistingOnLocalButNotOnServer();
			}else{
				throw new RuntimeException(localDir.getAbsolutePath() + "is not a directory");
			}
		}catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void findFilesExistingOnBothButServerUpToDate() {
		for(File serverFile : serverDir.listFiles()){
			if(existFileName(serverFile.getName(), localDir.listFiles())){
				File localFile = findFileByName(serverFile.getName(), localDir.listFiles());
				Long serverFileLastMod = serverFile.lastModified();
				Long localFileLastMod = localFile.lastModified();
				if(serverFileLastMod.compareTo(localFileLastMod) > 0){
					toDownload.add(localFile);
				}
			}
		}
	}

	private void findFilesExistingOnBothButClientUpToDate() {
		for(File localFile : localDir.listFiles()){
			if(!localFile.isDirectory() && existFileName(localFile.getName(), serverDir.listFiles())){
				File serverFile = findFileByName(localFile.getName(), serverDir.listFiles());
				Long localFileLastMod = localFile.lastModified();
				Long serverFileLastMod = serverFile.lastModified();
				if(localFileLastMod.compareTo(serverFileLastMod) > 0){
					toUpload.add(localFile);
				}
			}
		}
	}

	private File findFileByName(String fileName, File[]fileList){
		for(File file : fileList){
			if(file.getName().equals(fileName)){
				return file;
			}
		}
		return null;
	}
	private void findFilesExistingOnLocalButNotOnServer() {
		if(serverDir.exists()){
			for(File file : localDir.listFiles()){
				if(!file.isDirectory() && !existFileName(file.getName(), serverDir.listFiles())){
					toUpload.add(file);
				}
			}
		}else{
			toUpload.addAll(Arrays.asList(localDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return !pathname.isDirectory();
				}
			})));
		}
	}

	private void findFilesExistingOnServerButNotOnLocal() {
		for(File file : serverDir.listFiles()){
			if(!existFileName(file.getName(), localDir.listFiles())){
				toDownload.add(new File(localDir, file.getName()));
			}
		}
	}

	private boolean existFileName(String fileName, File[] fileList){
		for(File file : fileList){
			if(file.getName().equals(fileName)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void run() {
		System.out.println("Calculando diferenças");
		calculateDifferences();
		System.out.println("Iniciando sincronização");
		executor.execute(new UploadExecutor(toUpload, fileSync));
		executor.execute(new DownloadExecutor(toDownload, fileSync));
		executor.shutdown();
	}
}
