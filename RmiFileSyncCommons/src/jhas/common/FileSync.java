package jhas.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface FileSync extends Remote{
	public byte[] download(FileDto fileDto) throws RemoteException;
	public void update(byte[] fileBytes, FileDto fileDto) throws RemoteException;
	public List<FileDto> getDirectoryDescription(String dirPath) throws RemoteException;
}
