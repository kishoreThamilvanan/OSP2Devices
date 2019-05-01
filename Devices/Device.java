package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{
    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
    public Device(int id, int numberOfBlocks)
    {

	    super(id,numberOfBlocks);
	    iorbQueue = (GenericQueueInterface) new GenericList();
	    
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
        // your code goes here

    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
        
    	/**
    	 * locking the page associated with iorb
    	 */
    	
    	iorb.getPage().lock(iorb);
    	iorb.getOpenFile().incrementIORBCount();
    	
    	/**
    	 * to calculate number of blocks in a track.
    	 */
    	
    	// tracks -> blocks -> sectors 
    	int bytes_in_a_block = ((int) Math.pow(2, (MMU.getVirtualAddressBits() - MMU.getPageAddressBits())));
    	
    	int blocks_in_a_sector = bytes_in_a_block/((Disk)this).getBytesPerSector();
    	
    	int blocks_in_a_track = ((Disk)this).getSectorsPerTrack()/blocks_in_a_sector;
    	
    	int cylinder = (int)( iorb.getBlockNumber() / (blocks_in_a_track * ((Disk)this).getPlatters()));
    	
    	// setting the cylinder
    	iorb.setCylinder(cylinder);
    	
    	if(iorb.getThread().getStatus() == ThreadKill)
    		return FAILURE;
    	
    	if(isBusy()) {
    		((GenericList) iorbQueue).append(iorb);
    		return SUCCESS;
    	}
    	
    	// if the device is IDLE
    	startIO(iorb);
    	return SUCCESS;
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
    	
    	//is the queue is empty then return null
    	if(iorbQueue.isEmpty())
    		return null;
    
    	return  (IORB)(((GenericList) iorbQueue).removeHead());
    	
    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
    	IORB tiorb = null;
        
    	if(thread.getStatus() == ThreadKill) {
	    	int i=-1;
	    	while(++i<iorbQueue.length()) {
	    
	    		tiorb = (IORB)(((ArrayList) iorbQueue).get(i));
	    		
	    		// for each IORB associated thread.
	    		if(tiorb.getThread() == thread) {
	    			
	    			tiorb.getPage().unlock();
	    			tiorb.getOpenFile().decrementIORBCount();
	    			
	    			if(tiorb.getOpenFile().getIORBCount() == 0)
	    				if(tiorb.getOpenFile().closePending)
	    					tiorb.getOpenFile().close();
	    		
	    			((ArrayList) iorbQueue).remove(tiorb);
	    		}	    		
	    	}
       	}
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
