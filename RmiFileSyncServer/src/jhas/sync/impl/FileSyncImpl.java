package jhas.sync.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jhas.common.Constants;
import jhas.common.FileDto;
import jhas.common.FileSync;

public class FileSyncImpl extends UnicastRemoteObject implements FileSync {
	private static final long serialVersionUID = 1L;

	public FileSyncImpl() throws RemoteException {
		super();
	}

	@Override
	public byte[] download(FileDto fileDto) throws RemoteException{
		File serverFile = new File(Constants.SYNC_FOLDER + fileDto.getParentName(), fileDto.getName());
		byte[] fileBytes = null;
		if(Files.exists(serverFile.toPath())){
			try {
				fileBytes = Files.readAllBytes(serverFile.toPath());
				System.out.printf("%s downloaded - %s\n", serverFile.getName(), new Date());
			} catch (IOException e) {
				e.printStackTrace();
				throw new RemoteException(e.getMessage());
			}
		}
		return fileBytes;
	}

	@Override
	public void update(byte[] fileBytes, FileDto fileDto) throws RemoteException{
		File serverFile = new File(Constants.SYNC_FOLDER + fileDto.getParentName(), fileDto.getName());
		File backup = null;
		try{
			if(!Files.exists(serverFile.toPath().getParent())){
				Files.createDirectories(serverFile.toPath().getParent());
			}
			if(Files.exists(serverFile.toPath(), LinkOption.NOFOLLOW_LINKS)){
				backup = createFileBackup(serverFile);
			}
			Files.write(serverFile.toPath(), fileBytes);
			System.out.printf("%s uploaded - %s\n", serverFile.getName(), new Date());
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
	public List<FileDto> getDirectoryDescription(String dirPath) throws RemoteException{
		File dir = new File(Constants.SYNC_FOLDER + dirPath);
		if(dir.exists() && dir.isDirectory()){
			List<FileDto> dtos = new ArrayList<>();
			for(File file : dir.listFiles()){
				if( file.isFile()){
					FileDto dto = new FileDto(file.getName(), new Date(file.lastModified()), file.getPath());
					dtos.add(dto);
				}
			}
			return dtos;
		}
		return null;
	}
}
