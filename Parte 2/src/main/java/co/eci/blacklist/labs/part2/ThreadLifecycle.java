package co.eci.blacklist.labs.part2;

import java.util.ArrayList;
import java.util.List;

import co.eci.blacklist.infrastructure.HostBlackListsDataSourceFacade;

public class ThreadLifecycle extends Thread{
    private final int startIndex;
    private final int endIndex;
    private final String ip;
    private final List<Integer> occurrences = new ArrayList<>();

    public ThreadLifecycle(int startIndex, int endIndex, String ipAddress) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.ip = ipAddress;
        setName("ThreadLifeCycle-" + startIndex + "-" + endIndex);
    }

    @Override
    public void run(){
        HostBlackListsDataSourceFacade facade = HostBlackListsDataSourceFacade.getInstance();
        for (int i = startIndex; i < endIndex; i++){
            if (facade.isInBlackListServer(i, ip)){
                occurrences.add(i);
            }
        }

    }
    
    /*
     * Returns the list of server indexes where the IP was found
     */


    public List<Integer> getOcurrences(){
        return occurrences;
    }

    /*
     * Returns the number of events found
     */

    public int getOcurrencesCount(){
        return occurrences.size();
    }

}
