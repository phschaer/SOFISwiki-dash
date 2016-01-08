/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package wikiauswertung;

import java.util.TreeMap;

/**
 *
 * @author nico
 */
public class CountWikiSession {
    
    private String dashBoard;
    private String sessionKey;
    private TreeMap<String, Double> sessionActionsDistribution;
    private String timeGroup;

    CountWikiSession(String sessionKey, String dashboard, TreeMap<String, Double> sessionActionsDistribution, String timeGroup) {
        this.sessionKey = sessionKey;
        this.dashBoard = dashboard;
        this.timeGroup = timeGroup;
        this.sessionActionsDistribution = sessionActionsDistribution;
    }

    /**
     * @return the dashBoard
     */
    public String getDashBoard() {
        return dashBoard;
    }

    /**
     * @param dashBoard the dashBoard to set
     */
    public void setDashBoard(String dashBoard) {
        this.dashBoard = dashBoard;
    }

    /**
     * @return the sessionKey
     */
    public String getSessionKey() {
        return sessionKey;
    }

    /**
     * @param sessionKey the sessionKey to set
     */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    /**
     * @return the timeGroup
     */
    public String getTimeGroup() {
        return timeGroup;
    }

    /**
     * @param timeGroup the timeGroup to set
     */
    public void setTimeGroup(String timeGroup) {
        this.timeGroup = timeGroup;
    }

    /**
     * @return the sessionActionsDistribution
     */
    public TreeMap<String, Double> getSessionActionsDistribution() {
        return sessionActionsDistribution;
    }

    /**
     * @param sessionActionsDistribution the sessionActionsDistribution to set
     */
    public void setSessionActionsDistribution(TreeMap<String, Double> sessionActionsDistribution) {
        this.sessionActionsDistribution = sessionActionsDistribution;
    }
    
}
