/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikidash;

import java.util.Date;

/**
 *
 * @author nico
 */
public class Action implements Comparable {

    private String action;
    private Date timestamp;
    private String dashboard;
    private String user;
    
    public Action(String action, String dashboard) {
        this.action = action;
        this.dashboard = dashboard;
    }

    public Action(String action, Date timestamp, String dashboard, String user) {
        this.action = action;
        this.timestamp = timestamp;
        this.dashboard = dashboard;
        this.user = user;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the dashboard
     */
    public String getDashboard() {
        return dashboard;
    }

    /**
     * @param dashboard the dashboard to set
     */
    public void setDashboard(String dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    
    @Override
    public int compareTo(Object o) {
        Action otherAction = (Action) o;
        int timeComp = this.timestamp.compareTo(otherAction.getTimestamp());
        if(timeComp != 0){
            return timeComp;
        } 
        int actionComp = this.action.compareTo(otherAction.getAction());
        if(actionComp != 0){
            return actionComp;
        } else {
            return this.dashboard.compareTo(otherAction.getDashboard());
        }
        
    }

    @Override
    public String toString() {
        return this.user + ";" + this.timestamp + ";" + this.action + ";" + this.dashboard;
    }
    
}
