/***************************************************************************
 *                                                                         *
 *                               NodeImpl.java                             *
 *                            -------------------                          *
 *   date                 : 16.08.2004                                     *
 *   copyright            : (C) 2004-2008 Distributed and                  *
 *                              Mobile Systems Group                       *
 *                              Lehrstuhl fuer Praktische Informatik       *
 *                              Universitaet Bamberg                       *
 *                              http://www.uni-bamberg.de/pi/              *
 *   email                : sven.kaffille@uni-bamberg.de                   *
 *   			    		karsten.loesing@uni-bamberg.de                 *
 *                                                                         *
 *                                                                         *
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   A copy of the license can be found in the license.txt file supplied   *
 *   with this software or at: http://www.gnu.org/copyleft/gpl.html        *
 *                                                                         *
 ***************************************************************************/
package de.uniba.wiai.lspi.chord.service.impl;

import de.uniba.wiai.lspi.chord.com.*;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.util.logging.Logger;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.DEBUG;
import static de.uniba.wiai.lspi.util.logging.Logger.LogLevel.INFO;

/**
 * Implements all operations which can be invoked remotely by other nodes.
 * 
 * @author Karsten Loesing
 * @version 1.0.5
 */
public final class NodeImpl extends Node {

	/**
	 * Endpoint for incoming communication.
	 */
	private Endpoint myEndpoint = null;

	/**
	 * Reference on local node.
	 */
	private ChordImpl impl;

	/**
	 * Object logger.
	 * The name of the logger is the name of this class with the nodeID appended. 
	 * The length of the nodeID depends on the number of bytes that are displayed 
	 * when the ID is shown in Hex-Representation. See documentation of {@link ID}. 
	 * E.g. de.uniba.wiai.lspi.chord.service.impl.NodeImpl.FF FF FF FF if the number 
	 * of displayed Bytes of an ID is 4. 
	 */
	private Logger logger;

	/**
	 * Routing table (including finger table, successor list, and predecessor
	 * reference)
	 */
	private References references;

	/**
	 * Repository for locally stored entries.
	 */
	private Entries entries;

	/**
	 * Executor that executes insertion and removal of entries on successors of
	 * this node.
	 */
	private Executor asyncExecutor;
	
	private Lock notifyLock;

    private Integer transaction;

	/**
	 * Creates that part of the local node which answers remote requests by
	 * other nodes. Sole constructor, is invoked by ChordImpl only.
	 * 
	 * @param impl
	 *            Reference on ChordImpl instance which created this object.
	 * @param nodeID
	 *            This node's Chord ID.
	 * @param nodeURL
	 *            URL, on which this node accepts connections.
	 * @param references
	 *            Routing table of this node.
	 * @param entries
	 *            Repository for entries of this node.
	 * @throws IllegalArgumentException
	 *             If any of the parameter has value <code>null</code>.
	 */
	NodeImpl(ChordImpl impl, ID nodeID, URL nodeURL, NotifyCallback nodeCallback, References references,
			Entries entries) {

		if (impl == null || nodeID == null || nodeURL == null
				|| references == null || entries == null || nodeCallback == null) {
			throw new IllegalArgumentException(
					"Parameters of the constructor may not have a null value!");
		}

		this.logger = Logger.getLogger(NodeImpl.class.getName() + "." + nodeID.toString());

		this.impl = impl;
		this.asyncExecutor = impl.getAsyncExecutor();
		this.nodeID = nodeID;
		this.nodeURL = nodeURL;
		this.notifyCallback = nodeCallback;
		this.references = references;
		this.entries = entries;
		this.notifyLock = new ReentrantLock(true); 
		
		// create endpoint for incoming connections
		this.myEndpoint = Endpoint.createEndpoint(this, nodeURL);
		this.myEndpoint.listen();
        this.transaction = new Integer(0);
	}

	/**
	 * Makes this endpoint accept entries by other nodes. Is invoked by
	 * ChordImpl only.
	 */
	final void acceptEntries() {
		this.myEndpoint.acceptEntries();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void disconnect() {
		this.myEndpoint.disconnect();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Node findSuccessor(ID key) {
		return this.impl.findSuccessor(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final List<Node> notify(Node potentialPredecessor) {
		/*
		 * Mutual exclusion between notify and notifyAndCopyEntries. 17.03.2008. sven.
		 */
		this.notifyLock.lock(); 
		try {
			// the result will contain the list of successors as well as the
			// predecessor of this node
			List<Node> result = new LinkedList<Node>();

			// add reference on predecessor as well as on successors to result
			if (this.references.getPredecessor() != null) {
				result.add(this.references.getPredecessor());
			} else {
				result.add(potentialPredecessor); 
			}
			result.addAll(this.references.getSuccessors());

//			 add potential predecessor to successor list and finger table and
			// set
			// it as predecessor if no better predecessor is available
			this.references.addReferenceAsPredecessor(potentialPredecessor);			
			return result;
		} finally {
			this.notifyLock.unlock(); 
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final RefsAndEntries notifyAndCopyEntries(Node potentialPredecessor)
			throws CommunicationException {
		/*
		 * Mutual exclusion between notify and notifyAndCopyEntries. 17.03.2008. sven.
		 */
		this.notifyLock.lock(); 
		try {
			// copy all entries which lie between the local node ID and the ID
			// of
			// the potential predecessor, including those equal to potential
			// predecessor
			Set<Entry> copiedEntries = this.entries.getEntriesInInterval(
					this.nodeID, potentialPredecessor.getNodeID());

			return new RefsAndEntries(this.notify(potentialPredecessor),
					copiedEntries);
		} finally {
			this.notifyLock.unlock(); 
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void ping() {
		// do nothing---returning of method is proof of live
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void insertEntry(Entry toInsert) throws CommunicationException {
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Inserting entry with id " + toInsert.getId()
					+ " at node " + this.nodeID);
		}

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if ((this.references.getPredecessor() == null)
				|| !toInsert.getId().isInInterval(
						this.references.getPredecessor().getNodeID(),
						this.nodeID)) {
			this.references.getPredecessor().insertEntry(toInsert); 
			return;
		}

		// add entry to local repository
		this.entries.add(toInsert);

		// create set containing this entry for insertion of replicates at all
		// nodes in successor list
		Set<Entry> newEntries = new HashSet<Entry>();
		newEntries.add(toInsert);

		// invoke insertReplicates method on all nodes in successor list
		final Set<Entry> mustBeFinal = new HashSet<Entry>(newEntries);
		for (final Node successor : this.references.getSuccessors()) {
			this.asyncExecutor.execute(new Runnable() {
				public void run() {
					try {
						successor.insertReplicas(mustBeFinal);
					} catch (CommunicationException e) {
						// do nothing
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void insertReplicas(Set<Entry> replicatesToInsert) {
		this.entries.addAll(replicatesToInsert);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void removeEntry(Entry entryToRemove)
			throws CommunicationException {

		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("Removing entry with id " + entryToRemove.getId()
					+ " at node " + this.nodeID);
		}

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if (this.references.getPredecessor() != null
				&& !entryToRemove.getId().isInInterval(
						this.references.getPredecessor().getNodeID(),
						this.nodeID)) {
			this.references.getPredecessor().removeEntry(entryToRemove);
			return;
		}

		// remove entry from repository
		this.entries.remove(entryToRemove);

		// create set containing this entry for removal of replicates at all
		// nodes in successor list
		final Set<Entry> entriesToRemove = new HashSet<Entry>();
		entriesToRemove.add(entryToRemove);

		// invoke removeReplicates method on all nodes in successor list
		List<Node> successors = this.references.getSuccessors();
		final ID id = this.nodeID;
		for (final Node successor : successors) {
			this.asyncExecutor.execute(new Runnable() {
				public void run() {
					try {
						// remove only replica of removed entry
						successor.removeReplicas(id, entriesToRemove);
					} catch (CommunicationException e) {
						// do nothing for the moment
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void removeReplicas(ID sendingNodeID,
			Set<Entry> replicasToRemove) {
		if (replicasToRemove.size() == 0) {
			// remove all replicas in interval
			boolean debug = this.logger.isEnabledFor(DEBUG);
			if (debug) {
				this.logger.debug("Removing replicas. Current no. of entries: "
						+ this.entries.getNumberOfStoredEntries());
			}
			/*
			 * Determine entries to remove. These entries are located between
			 * the id of the local peer and the argument sendingNodeID
			 */
			Set<Entry> allReplicasToRemove = this.entries.getEntriesInInterval(
					this.nodeID, sendingNodeID);
			if (debug) {
				this.logger.debug("Replicas to remove " + allReplicasToRemove);
				this.logger.debug("Size of replicas to remove "
						+ allReplicasToRemove.size());
			}

			/*
			 * Remove entries
			 */
			this.entries.removeAll(allReplicasToRemove);

			if (debug) {
				this.logger
						.debug("Removed replicas??? Current no. of entries: "
								+ this.entries.getNumberOfStoredEntries());
			}
		} else {
			// remove only replicas of given entry
			this.entries.removeAll(replicasToRemove);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Set<Entry> retrieveEntries(ID id)
			throws CommunicationException {

		// Possible, but rare situation: a new node has joined which now is
		// responsible for the id!
		if ( (this.references.getPredecessor() != null)
			  && (!id.isInInterval(this.references.getPredecessor().getNodeID(), this.nodeID)) 
			  && (!this.nodeID.equals(id)) ) {
			this.logger.fatal("The rare situation has occured at time "
					+ System.currentTimeMillis() + ", id to look up=" + id
					+ ", id of local node=" + this.nodeID
					+ ", id of predecessor="
					+ this.references.getPredecessor().getNodeID());
			return this.references.getPredecessor().retrieveEntries(id);
		}
		// added by INET
		if (this.notifyCallback != null) {
			notifyCallback.retrieved(id);
		}
		// return entries from local repository
		// for this purpose create a copy of the Set in order to allow the
		// thread retrieving the entries to modify the Set without modifying the
		// internal Set of entries. sven
		return this.entries.getEntries(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final public void leavesNetwork(Node predecessor) {
		if (this.logger.isEnabledFor(INFO)) {
			this.logger.info("Leaves network invoked; " + this.nodeID
					+ ". Updating references.");
			this.logger.info("New predecessor " + predecessor.getNodeID());
		}
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("References before update: "
					+ this.references.toString());
		}
		this.references.removeReference(this.references.getPredecessor());
		if (this.logger.isEnabledFor(DEBUG)) {
			this.logger.debug("References after update: "
					+ this.references.toString());
		}
	}

	/**
	 * gets broadcast info with:
     * ID range - send broadcast up to this ID
     * ID source - attacking player/node
     * ID target - key/HashID of attacked player
     * Integer transaction - logical watch
     * Boolean hit - ship hit or not
     *
	 * @return
	 */

//	@Override
//	public final void broadcast(Broadcast info1) throws CommunicationException {
//		final Broadcast info=info1;
//		  // asynchronos send broadcast message to current node
//
//		List<Node> ft=impl.getFingerTable();
//		Collections.sort(ft);
//		
//		if(info1.getTransaction() > impl.getTransactionId()){
//			impl.setTransactionId(info1.getTransaction());
//			System.out.println("Informing the fleet!");
//		    final Broadcast message=info1;
//		    Node currentNode = ft.get(0);
//			  (new Thread(){
//		    	public void run(){
//		    	try { 
//					currentNode.broadcast(message); 
//					
//					
//				} catch (CommunicationException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}}
//		    }).start();
//		} else {
//			System.out.println("End this!");
//		}
//		// finally inform application
//		if (this.notifyCallback != null) {
//			this.notifyCallback.broadcast(info.getSource(), info.getTarget(), info.getHit());
//		}
//	}
	// TODO: implement this function in TTP
	
	public final void broadcast(Broadcast info) throws CommunicationException {
        try{
	        Integer transactionID = info.getTransaction().intValue();
	
	        ID ownID = this.getNodeID();
	        ID rangeID = info.getRange();
	        List<Node> nodes = impl.getFingerTable();
	        Collections.sort(nodes);
	
	        //maxID > range > ownId -> normal
	        
	        if(info.getTransaction() > impl.getTransactionId()){
				impl.setTransactionId(info.getTransaction());
				System.out.println("Informing the fleet!");
	        	
				int ringIdLimitCheck= ownID.compareTo(rangeID);
				
				if (ringIdLimitCheck == -1){
					System.out.println("Still in ring id limit");
					for(int i = 0; i < nodes.size(); i++){
						// if successor of current node is larger than the given range
	                    // send given range to current node and break
	                    if (nodes.get(i + 1).getNodeID().compareTo(rangeID) == 1) {
	                    	System.out.println(">1");
	                        sendBroadcastToNode(nodes.get(i), info, rangeID, transactionID);
	                        i = nodes.size();
	                        // send current node the broadcast with range up to successor node (in finger table)
	                    } else {
	                    	System.out.println(">2");
	                        ID newRange = ID.valueOf(nodes.get(i + 1).getNodeID().toBigInteger().subtract(BigInteger.ONE));
	                        sendBroadcastToNode(nodes.get(i), info, newRange, transactionID);
	                    }
		        	}
				} else if (ringIdLimitCheck == 1){
					System.out.println("Past ring id limit");
					
					
	                ID newRange;
	                //there are two types of nodes here
	                // node > range //from here to max
	                //range > node //from zero to range
	                // all nodes got their successor in fingertable as range
	                // the node with the highest value get the first (lowest) fingertable-entry (last node in fingertable)
	                // the last node with node < range < ownID got the range (successor is node > ownID > range)
	                //ignore the nodes with range < node < ownID	                
	                for (int i = 0; i < nodes.size(); i++) {	                    
	                	ID currentNode = nodes.get(i).getNodeID();
	                    
	                    //ignore the nodes with range < node < ownID
	                    if ((rangeID.compareTo(currentNode) == -1) && (currentNode.compareTo(ownID) == -1)) {
	                    	System.out.println("~1");
	                        //do nothing and do not break the loop
	                    } else if (i == nodes.size() - 1) { //last entry receives the range to the first entry
	                    	System.out.println("~2");
	                        newRange = ID.valueOf(nodes.get(0).getNodeID().toBigInteger().subtract(BigInteger.ONE));
	                        sendBroadcastToNode(nodes.get(i), info, newRange, transactionID);
	                    } else if ((currentNode.compareTo(rangeID) == -1) && (/*currentSuccessor*/nodes.get(i+1).getNodeID().compareTo(rangeID) == 1)) { //last node to receive broadcast
	                    	System.out.println("~3");
	                        sendBroadcastToNode(nodes.get(i), info, rangeID, transactionID);
	                    } else { //all the other, normal nodes
	                    	System.out.println("~4");
	                        newRange = ID.valueOf(/*currentSuccessor*/nodes.get(i+1).getNodeID().toBigInteger().subtract(BigInteger.ONE));
	                        sendBroadcastToNode(nodes.get(i), info, newRange, transactionID);
	                    }
	                }
				} else { //if (ringIdLimitCheck = 0)
					System.out.println("Somethings wrong! This can not happen.");
					
				}
		    } else {
				System.out.println("Already got this information!");
			}
			
		    // finally inform application
		    if (this.notifyCallback != null && !info.getSource().equals(this.getNodeID())) {
		        this.notifyCallback.broadcast(info.getSource(), info.getTarget(), info.getHit());
		    }
        } catch (Exception e){
        	System.out.println("Exception during Broadcast!");
        	e.printStackTrace();
        }
	}

    private void sendBroadcastToNode(Node node, Broadcast info, ID range, Integer transaction){
    	if(!node.getNodeID().equals(info.getTarget())){ //Do not send when destination is target node to avoid double information
	    	System.out.println("Sending to: "+node.getNodeID());
	    	(new Thread(){
		    	public void run(){
		            try {
		                node.broadcast(new Broadcast(
		                        range, info.getSource(), info.getTarget(), transaction, info.getHit()
		                ));
		            } catch (CommunicationException e) {
		                e.printStackTrace();
		            }
		    	}
		    }).start();
    	} else {
    		System.out.println("Stop. Target already knows this.");
    	}
    }

}