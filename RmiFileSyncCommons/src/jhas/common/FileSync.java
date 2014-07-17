package jhas.common;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileSync extends Remote{
	public byte[] download(File file) throws RemoteException;
	public void update(byte[] fileBytes, File file) throws RemoteException;
	public File getDirectoryDescription(String dirPath) throws RemoteException;
}
