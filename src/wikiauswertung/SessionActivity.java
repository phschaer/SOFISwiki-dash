/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiauswertung;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author nico
 */
public class SessionActivity {
    
    static void createActivityDistribution(ArrayList<WikiSession> dashboardSessions, LinkedList<String[]> conditions, int stepLength) {
        for (String action : SessionConstruction.relevantActions) {
            createDistributionForAction(dashboardSessions, conditions, stepLength, action);
        }
    }

    private static void createDistributionForAction(ArrayList<WikiSession> dashboardSessions, LinkedList<String[]> conditions, int stepLength, String action) {
        for (String[] cond : conditions) {
            createDistribution(dashboardSessions, stepLength, action, cond);
        }
    }

    private static void createDistribution(ArrayList<WikiSession> dashboardSessions, int stepLength, String action, String[] condition) {
        
        TreeMap<Integer, Integer> activityDistribution = new TreeMap<>();
        
        String conditionName;
        if (condition[0].equals("NONE")) {
            conditionName = "NONE";
        } else {
            conditionName = "DASHBOARDS";
        }

        for (WikiSession session : dashboardSessions) {
            ArrayList<Action> actions = session.getActions();
            if (Arrays.asList(condition).contains(session.getDashBoard().split("/sofiswiki/Spezial:GesisDashboard")[1])) {
                Collections.sort(actions);
                long sessionStart = actions.get(0).getTimestamp().getTime();
                for (Action a : actions) {
                    if (a.getAction().equals(action)) {
                        Integer diff = (int) TimeUnit.MILLISECONDS.toMinutes(a.getTimestamp().getTime() - sessionStart);
                        if(!activityDistribution.containsKey(diff)){
                            activityDistribution.put(diff, 1);
                        } else {
                            activityDistribution.put(diff, activityDistribution.get(diff)+1);
                        }
                    }
                }
            }
        }
        SmirnovEvaluation.dumpDistributionToFile(activityDistribution, conditionName+"_"+action+".csv");
        // dump distribution_action here
    }

}
