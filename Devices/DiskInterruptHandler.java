package osp.Devices;
/**
 * Name: Kishore Thamilvanan
 * ID  : 111373510
 * 
 * I pledge my honor that all parts of this project were done by me individually, 
 * without collaboration with anyone, and without consulting external 
 * sources that help with similar projects.
 */
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
      
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
    	
    	
    	

		/*
		 * 1. obtain info about the interrupt.
		 */
    	
    	IORB iorb = (IORB) (InterruptVector.getEvent());
    	
    	
    	OpenFile iorbOpenFile = iorb.getOpenFile();
    	
		/*
		 * 2. decrementIORBCount()
		 */
    	
    	iorbOpenFile.decrementIORBCount();
    	
		/*
		 * 3. 
		 */    	
    	
    	if(iorbOpenFile.closePending && iorbOpenFile.getIORBCount() == 0) 
    		iorbOpenFile.close();
    	
		/*
		 * 4. The page associated with the IORB must be unlocked, because the I/O 
		 * 		operation (due to which the page was locked) is over. 
		 */
    	
    	PageTableEntry iorbPage = iorb.getPage();
    	iorbPage.unlock();
    	
		/*
		 * 5. 
		 * 
		 */
    	
    	FrameTableEntry iorbPageFrame = iorbPage.getFrame();
    	ThreadCB iorbThread = iorb.getThread();
    	
    	if(iorb.getDeviceID() != SwapDeviceID)
    		if(iorbThread.getStatus() != ThreadKill) {
    			
    			iorbPageFrame.setReferenced(true);
    			
    			if(iorb.getIOType() == FileRead)
    				if(iorbThread.getTask().getStatus() == TaskLive)
    					iorbPageFrame.setDirty(true);
    		}
    	
    	
		/*
		 * 6.
		 */
    	
    	if(iorb.getDeviceID() == SwapDeviceID && iorbThread.getTask().getStatus() == TaskLive)
    		iorbPageFrame.setDirty(false);
    	
    	/*
		 * 7.
		 */
    	
    	if(iorbThread.getTask().getStatus() == TaskTerm && iorbThread.getTask() == iorbPageFrame.getReserved())
    		iorbPageFrame.setUnreserved(iorbThread.getTask());
    	
    	/*
		 * 8.
		 */
    	iorb.notifyThreads();
    	
    	/*
		 * 9.
		 */
    	Device.get(iorb.getDeviceID()).setBusy(false);
    	
    	/*
		 * 10.
		 */
    	IORB newIOrequest = Device.get(iorb.getDeviceID()).dequeueIORB();
    	
    	if(newIOrequest != null)
    		Device.get(newIOrequest.getDeviceID()).startIO(newIOrequest);
    	
    	
    	/*
		 * 11.
		 */
    	iorbThread.dispatch();
    	
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
