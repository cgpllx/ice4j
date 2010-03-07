/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 * Maintained by the SIP Communicator community (http://sip-communicator.org).
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.ice4j.ice;

import java.util.*;
import java.util.logging.*;

/**
 * <tt>CandidatePair</tt>s map local to remote <tt>Candidate</tt>s so that they
 * could be added to check lists. Connectivity in ICE is always verified by
 * pairs: i.e. STUN packets are sent from the local candidate of a pair to the
 * remote candidate of a pair. To see which pairs work, an agent schedules a
 * series of <tt>ConnectivityCheck</tt>s. Each check is a STUN request/response
 * transaction that the client will perform on a particular candidate pair by
 * sending a STUN request from the local candidate to the remote candidate.
 *
 * @author Emil Ivov
 */
public class CandidatePair
    implements Comparable<CandidatePair>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CandidatePair</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(CandidatePair.class.getName());

    /**
     * The local candidate of this pair.
     */
    private LocalCandidate localCandidate;

    /**
     * The remote candidate of this pair.
     */
    private Candidate remoteCandidate;

    /**
     * Priority of the candidate-pair
     */
    private final long priority;

    /**
     * A <tt>Comparator</tt> using the <tt>compareTo</tt> method of the
     * <tt>CandidatePair</tt>.
     */
    public static final PairComparator comparator = new PairComparator();

    /**
     * Each candidate pair has a state that is assigned once the check list
     * for each media stream has been computed. The ICE RFC defines five
     * potential values that the state can have and they are all represented
     * in the <tt>CandidatePairState</tt> enumeration. The ICE spec stipulates
     * that the first step of the state initialization process is: The agent
     * sets all of the pairs in each check list to the Frozen state, and hence
     * our default state.
     */
    private CandidatePairState state = CandidatePairState.FROZEN;

    /**
     * Creates a <tt>CandidatePair</tt> instance mapping <tt>localCandidate</tt>
     * to <tt>remoteCandidate</tt>.
     *
     * @param localCandidate the local candidate of the pair.
     * @param remoteCandidate the remote candidate of the pair.
     */
    public CandidatePair(LocalCandidate localCandidate,
                         Candidate      remoteCandidate)
    {

        this.localCandidate = localCandidate;
        this.remoteCandidate = remoteCandidate;

        priority = computePriority();
    }

    /**
     * Returns the foundation of this <tt>CandidatePair</tt>. The foundation
     * of a <tt>CandidatePair</tt> is just the concatenation of the foundations
     * of its two candidates. Initially, only the candidate pairs with unique
     * foundations are tested. The other candidate pairs are marked "frozen".
     * When the connectivity checks for a candidate pair succeed, the other
     * candidate pairs with the same foundation are unfrozen. This avoids
     * repeated checking of components which are superficially more attractive
     * but in fact are likely to fail.
     *
     * @return the foundation of this candidate pair, which is a concatenation
     * of the foundations of the remote and local candidates.
     */
    public String getFoundation()
    {
        return localCandidate.getFoundation()
            + remoteCandidate.getFoundation();
    }

    /**
     * Returns the <tt>LocalCandidate</tt> of this <tt>CandidatePair</tt>.
     *
     * @return the local <tt>Candidate</tt> of this <tt>CandidatePair</tt>.
     */
    public LocalCandidate getLocalCandidate()
    {
        return localCandidate;
    }

    /**
     * Sets the <tt>LocalCandidate</tt> of this <tt>CandidatePair</tt>.
     *
     * @param localCnd the local <tt>Candidate</tt> of this
     * <tt>CandidatePair</tt>.
     */
    protected void setLocalCandidate( LocalCandidate localCnd)
    {
        this.localCandidate = localCnd;;
    }

    /**
     * Returns the remote candidate of this <tt>CandidatePair</tt>.
     *
     * @return the remote <tt>Candidate</tt> of this <tt>CandidatePair</tt>.
     */
    public Candidate getRemoteCandidate()
    {
        return remoteCandidate;
    }

    /**
     * Sets the <tt>RemoteCandidate</tt> of this <tt>CandidatePair</tt>.
     *
     * @param remoteCnd the local <tt>Candidate</tt> of this
     * <tt>CandidatePair</tt>.
     */
    protected void setRemoteCandidate( Candidate remoteCnd)
    {
        this.remoteCandidate = remoteCnd;;
    }

    /**
     * Returns the state of this <tt>CandidatePair</tt>. Each candidate pair has
     * a state that is assigned once the check list for each media stream has
     * been computed. The ICE RFC defines five potential values that the state
     * can have. They are represented here with the <tt>CandidatePairState</tt>
     * enumeration.
     *
     * @return the <tt>CandidatePairState</tt> that this candidate pair is
     * currently in.
     */
    public CandidatePairState getState()
    {
        return state;
    }

    /**
     * Sets the <tt>CandidatePairState</tt> of this pair to <tt>state</tt>. This
     * method should only be called by the ice agent, during the execution of
     * the ICE procedures.
     *
     * @param state the state that this candidate pair is to enter.
     */
    protected void setState(CandidatePairState state)
    {
        this.state = state;
    }

    /**
     * Determines whether this candidate pair is frozen or not. Initially, only
     * the candidate pairs with unique foundations are tested. The other
     * candidate pairs are marked "frozen". When the connectivity checks for a
     * candidate pair succeed, the other candidate pairs with the same
     * foundation are unfrozen.
     *
     * @return true if this candidate pair is frozen and false otherwise.
     */
    public boolean isFrozen()
    {
        return this.getState().equals(CandidatePairState.FROZEN);
    }

    /**
     * Returns the candidate in this pair that belongs to the controlling agent.
     *
     * @return a reference to the <tt>Candidate</tt> instance that comes from
     * the controlling agent.
     */
    public Candidate getControllingAgentCandidate()
    {
        return (getLocalCandidate().getParentComponent().getParentStream()
                        .getParentAgent().isControlling())
                    ? getLocalCandidate()
                    : getRemoteCandidate();
    }

    /**
     * Returns the candidate in this pair that belongs to the controlled agent.
     *
     * @return a reference to the <tt>Candidate</tt> instance that comes from
     * the controlled agent.
     */
    public Candidate getControlledAgentCandidate()
    {
        return (getLocalCandidate().getParentComponent().getParentStream()
                        .getParentAgent().isControlling())
                    ? getRemoteCandidate()
                    : getLocalCandidate();
    }


    /**
     * A candidate pair priority is computed the following way:<br>
     * Let G be the priority for the candidate provided by the controlling
     * agent. Let D be the priority for the candidate provided by the
     * controlled agent. The priority for a pair is computed as:
     * <p>
     * <i>pair priority = 2^32*MIN(G,D) + 2*MAX(G,D) + (G>D?1:0)</i>
     * <p>
     * This formula ensures a unique priority for each pair. Once the priority
     * is assigned, the agent sorts the candidate pairs in decreasing order of
     * priority. If two pairs have identical priority, the ordering amongst
     * them is arbitrary.
     *
     * @return a long indicating the priority of this candidate pair.
     */
    private long computePriority()
    {
        //use G and D as local and remote candidate priority names to fit the
        //definition in the RFC.
        long G = getControllingAgentCandidate().getPriority();
        long D = getControlledAgentCandidate().getPriority();

        return (long)Math.pow(2, 32)*Math.min(G,D)
                + 2*Math.max(G,D)
                + (G>D?1l:0l);
    }

    /**
     * Returns the priority of this pair.
     *
     * @return the priority of this pair.
     */
    public long getPriority()
    {
        return priority;
    }

    /**
     * Compares this <tt>CandidatePair</tt> with the specified object for order.
     * Returns a negative integer, zero, or a positive integer as this
     * <tt>CandidatePair</tt>'s priority is greater than, equal to, or less than
     * the one of the specified object thus insuring that higher priority pairs
     * will come first.<p>
     *
     * @param   candidatePair the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this
     * <tt>CandidatePair</tt>'s priority is greater than, equal to, or less than
     * the one of the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(CandidatePair candidatePair)
    {
        long thisPri = getPriority();
        long otherPri = candidatePair.getPriority();

        return (thisPri < otherPri
                        ? 1
                        : (thisPri==otherPri
                                        ? 0
                                        : -1));
    }

    /**
     * Compares this <tt>CandidatePair</tt> to <tt>targetPair</tt> and returns
     * <tt>true</tt> if pairs have equal local and equal remote candidates and
     * <tt>false</tt> otherwise.
     *
     * @param targetPair the <tt>Object</tt> that we'd like to compare this
     * target pair to.
     *
     * @return <tt>true</tt> if pairs have equal local and equal remote
     * candidates and <tt>false</tt> otherwise.
     */
    public boolean equals(Object targetPair)
    {
        if (! (targetPair instanceof CandidatePair)
            || targetPair == null
            || !localCandidate.equals(((CandidatePair)targetPair)
                            .localCandidate)
            || !remoteCandidate.equals(((CandidatePair)targetPair)
                            .remoteCandidate))
            return false;

        return true;
    }

    /**
     * Returns a String representation of this <tt>CandidatePair</tt>.
     *
     * @return a String representation of the object.
     */
    public String toString()
    {
        return "CandidatePair (State=" + getState()
            + " Priority=" + getPriority()
            + "):\n\tLocalCandidate=" + getLocalCandidate()
            + "\n\tRemoteCandidate=" + getRemoteCandidate();
    }

    /**
     * A <tt>Comparator</tt> using the <tt>compareTo</tt> method of the
     * <tt>CandidatePair</tt>
     */
    public static class PairComparator implements Comparator<CandidatePair>
    {
        /**
         * Compares <tt>pair1</tt> and <tt>pair2</tt> for order. Returns a
         * negative integer, zero, or a positive integer as <tt>pair1</tt>'s
         * priority is greater than, equal to, or less than the one of the
         * pair2, thus insuring that higher priority pairs will come first.
         *
         * @param pair1 the first <tt>CandidatePair</tt> to be compared.
         * @param pair2 the second <tt>CandidatePair</tt> to be compared.
         *
         * @return  a negative integer, zero, or a positive integer as the first
         * pair's priority priority is greater than, equal to, or less than
         * the one of the second pair.
         *
         * @throws ClassCastException if the specified object's type prevents it
         *         from being compared to this Object.
         */
        public int compare(CandidatePair pair1, CandidatePair pair2)
        {
            return pair1.compareTo(pair2);
        }

        /**
         * Indicates whether some other object is &quot;equal to&quot; to this
         * Comparator.  This method must obey the general contract of
         * <tt>Object.equals(Object)</tt>.  Additionally, this method can return
         * <tt>true</tt> <i>only</i> if the specified Object is also a comparator
         * and it imposes the same ordering as this comparator.  Thus,
         * <code>comp1.equals(comp2)</code> implies that <tt>sgn(comp1.compare(o1,
         * o2))==sgn(comp2.compare(o1, o2))</tt> for every object reference
         * <tt>o1</tt> and <tt>o2</tt>.<p>
         *
         * Note that it is <i>always</i> safe <i>not</i> to override
         * <tt>Object.equals(Object)</tt>.  However, overriding this method may,
         * in some cases, improve performance by allowing programs to determine
         * that two distinct Comparators impose the same order.
         *
         * @param   obj   the reference object with which to compare.
         * @return  <code>true</code> only if the specified object is also
         *      a comparator and it imposes the same ordering as this
         *      comparator.
         * @see     java.lang.Object#equals(java.lang.Object)
         * @see java.lang.Object#hashCode()
         */
        public boolean equals(Object obj)
        {
            return obj instanceof PairComparator;
        }
    }

    /**
     * Returns the <tt>Component</tt> that this pair belongs to.
     *
     * @return the <tt>Component</tt> that this pair belongs to.
     */
    public Component getParentComponent()
    {
        return getLocalCandidate().getParentComponent();
    }
}
