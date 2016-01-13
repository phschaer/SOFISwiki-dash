/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikidash;

/**
 *
 * @author nico
 */
public class UniqueAction implements Comparable {

    private String action;
    private String url;
    
    public UniqueAction(String url, String user){
        this.action = url;
        this.url = user;
    }

    @Override
    public int compareTo(Object o) {
        UniqueAction a = (UniqueAction) o;
        if (this.getUrl().equals(a.getUrl()) && this.getAction().equals(a.getAction())) {
            return 0;
        } else {
            return 1;
        }
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
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

}
