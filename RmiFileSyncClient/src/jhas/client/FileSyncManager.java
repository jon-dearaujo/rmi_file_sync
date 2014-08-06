package jhas.client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import jhas.client.executors.DownloadExecutor;
import jhas.client.executors.UploadExecutor;
import jhas.common.Constants;
import jhas.common.FileDto;
import jhas.common.FileSync;

public class FileSyncManager implements Runnable{
	private List<FileDto> localDir;
	private String localDirPath;
	private List<FileDto> serverDirFiles;
	private FileSync fileSync;
	private List<FileDto> toUpload;
	private List<FileDto> toDownload;
	private ExecutorService executor;
	
	public FileSyncManager(File localDir, ExecutorService executor) {
		this.localDir = toFileDtoList(localDir);
		localDirPath = localDir.getPath();
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
	
	private List<FileDto> toFileDtoList(File localDir) {
		if(localDir.isDirectory()){
			List<FileDto> dtos = new ArrayList<>();
			for(File file : localDir.listFiles()){
				if(file.isFile()){
					dtos.add(new FileDto(file.getName(), new Date(file.lastModified()), file.getPath()));
				}
			}
			return dtos;
		}
		return null;
	}

	public void calculateDifferences(){
		try{
			if(localDir != null){
				serverDirFiles = fileSync.getDirectoryDescription(new File(localDirPath).getName());
				if(serverDirFiles != null){
					findFilesExistingOnServerButNotOnLocal();
					findFilesExistingOnBothButClientUpToDate();
					findFilesExistingOnBothButServerUpToDate();
				}
				findFilesExistingOnLocalButNotOnServer();
			}else{
				throw new RuntimeException(localDirPath + "is not a directory");
			}
		}catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void findFilesExistingOnBothButServerUpToDate() {
		for(FileDto serverFile : serverDirFiles){
			if(existFileName(serverFile.getName(), localDir)){
				FileDto localFile = findFileByName(serverFile.getName(), localDir);
				Date serverFileLastMod = serverFile.getLastModificationDate();
				Date localFileLastMod = localFile.getLastModificationDate();
				if(serverFileLastMod.compareTo(localFileLastMod) > 0){
					toDownload.add(localFile);
				}
			}
		}
	}

	private void findFilesExistingOnBothButClientUpToDate() {
		for(FileDto localFile : localDir){
			if(existFileName(localFile.getName(), serverDirFiles)){
				FileDto serverFile = findFileByName(localFile.getName(), serverDirFiles);
				Date localFileLastMod = localFile.getLastModificationDate();
				Date serverFileLastMod = serverFile.getLastModificationDate();
				if(localFileLastMod.compareTo(serverFileLastMod) > 0){
					toUpload.add(localFile);
				}
			}
		}
	}

	private FileDto findFileByName(String fileName,List<FileDto> fileList){
		for(FileDto file : fileList){
			if(file.getName().equals(fileName)){
				return file;
			}
		}
		return null;
	}
	private void findFilesExistingOnLocalButNotOnServer() {
		if(serverDirFiles != null){
			for(FileDto file : localDir){
				if(!existFileName(file.getName(), serverDirFiles)){
					toUpload.add(findFileByName(file.getName(), localDir));
				}
			}
		}else{
			toUpload.addAll(localDir);
		}
	}

	private void findFilesExistingOnServerButNotOnLocal() {
		for(FileDto fileDto : serverDirFiles){
			if(!existFileName(fileDto.getName(), localDir)){
				toDownload.add(new FileDto(fileDto.getName(), fileDto.getLastModificationDate(), 
						new File(localDirPath, fileDto.getName()).getPath()));
			}
		}
	}

	private boolean existFileName(String fileName, List<FileDto> fileList){
		for(FileDto file : fileList){
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
