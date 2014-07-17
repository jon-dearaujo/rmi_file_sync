package jhas.sync.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import jhas.common.Constants;
import jhas.common.FileSync;

public class FileSyncImpl extends UnicastRemoteObject implements FileSync {
	private static final long serialVersionUID = 1L;

	public FileSyncImpl() throws RemoteException {
		super();
	}

	@Override
	public byte[] download(File file) throws RemoteException{
		Path parentPath = file.toPath().getParent().getFileName();
		File serverFile = new File(Constants.SYNC_FOLDER + parentPath, file.getName());
		byte[] fileBytes = null;
		if(Files.exists(serverFile.toPath())){
			try {
				fileBytes = Files.readAllBytes(serverFile.toPath());
				System.out.printf("%s downloaded\n", serverFile.getName());
			} catch (IOException e) {
				e.printStackTrace();
				throw new RemoteException(e.getMessage());
			}
		}
		return fileBytes;
	}

	@Override
	public void update(byte[] fileBytes, File file) throws RemoteException{
		
		Path parentPath = file.toPath().getParent().getFileName();
		File serverFile = new File(Constants.SYNC_FOLDER + parentPath, file.getName());
		File backup = null;
		try{
			if(!Files.exists(serverFile.toPath().getParent())){
				Files.createDirectories(serverFile.toPath().getParent());
			}
			if(Files.exists(serverFile.toPath(), LinkOption.NOFOLLOW_LINKS)){
				backup = createFileBackup(serverFile);
			}
			Files.write(serverFile.toPath(), fileBytes);
			System.out.printf("%s uploaded\n", serverFile.getName());
		}catch(IOException e){
			e.printStackTrace();
			if(backup != null){
				restoreFileBackup(serverFile, backup);
			}
			throw new RemoteException(e.getMessage());
		}finally{
			if(backup != null){
				try {
					Files.delete(backup.toPath());
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage());
				}
			}
		}
	}

	private void restoreFileBackup(File file, File backup) throws RemoteException{
		try {
			Files.copy(backup.toPath(), file.toPath(), 
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			Files.delete(backup.toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RemoteException(e1.getMessage());
		}
	}

	private File createFileBackup(File file) throws IOException {
		Path fileParentPath = file.toPath().getParent().getFileName();
		File backup = new File(Constants.SYNC_FOLDER +fileParentPath, file.getName() + ".bak" );
		Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES, 
			StandardCopyOption.REPLACE_EXISTING);
		return backup;
	}

	@Override
	public File getDirectoryDescription(String dirPath) throws RemoteException{
		return new File(Constants.SYNC_FOLDER + dirPath);
	}
}
