/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package wikiauswertung;

import java.util.ArrayList;

/**
 *
 * @author nico
 */
public class WikiSession {
    
    private String dashBoard;
    private String sessionKey;
    private ArrayList<Action> actions;
    private String timeGroup;

    WikiSession(String sessionKey, String dashboard, ArrayList<Action> sessionActions, String timeGroup) {
        this.sessionKey = sessionKey;
        this.dashBoard = dashboard;
        this.actions = sessionActions;
        this.timeGroup = timeGroup;
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
     * @return the actions
     */
    public ArrayList<Action> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(ArrayList<Action> actions) {
        this.actions = actions;
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
    
    @Override
    public String toString(){
        String result = "";
        for(Action a:this.actions){
            result += this.sessionKey+";"+this.dashBoard+";"+a.toString()+"\n";
        }
        return result;
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
    
}
