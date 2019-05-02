package osp.Devices;
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
    	
		/*
		 * 2. decrementIORBCount()
		 */
    	
    	iorb.getOpenFile().decrementIORBCount();
    	
		/*
		 * 3. 
		 */    	
    	
    	if(iorb.getOpenFile().closePending && iorb.getOpenFile().getIORBCount() == 0) 
    		iorb.getOpenFile().close();
    	
		/*
		 * 4. The page associated with the IORB must be unlocked, because the I/O 
		 * 		operation (due to which the page was locked) is over. 
		 */
    	
    	iorb.getPage().unlock();
    	
		/*
		 * 5. 
		 * 
		 */
    	
    	if(iorb.getDeviceID() != SwapDeviceID)
    		if(iorb.getThread().getStatus() != ThreadKill) {
    			
    			iorb.getPage().getFrame().setReferenced(true);
    			if(iorb.getIOType() == FileRead)
    				if(iorb.getThread().getTask().getStatus() == TaskLive)
    					iorb.getPage().getFrame().setDirty(true);
    		}
    	
    	
		/*
		 * 6.
		 */
    	
    	if(iorb.getDeviceID() == SwapDeviceID && iorb.getThread().getTask().getStatus() == TaskLive)
    		iorb.getPage().getFrame().setDirty(false);
    	
    	/*
		 * 7.
		 */
    	
    	if(iorb.getThread().getTask().getStatus() == TaskTerm && iorb.getThread().getTask() == iorb.getPage().getFrame().getReserved())
    		iorb.getPage().getFrame().setUnreserved(iorb.getThread().getTask());
    	
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
    	iorb.getThread().dispatch();
    	
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
