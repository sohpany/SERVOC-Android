package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.EnumMap;

import static org.junit.Assert.*;

public class ChemCoreExperimentalCampaignMapTest {
    @Test public void all144EntriesReceiveOneCampaign(){
        EnumMap<ChemCoreExperimentalCampaignMap.Campaign,Integer> counts=new EnumMap<>(ChemCoreExperimentalCampaignMap.Campaign.class);
        for(ChemCoreExperimentalCampaignMap.Campaign c:ChemCoreExperimentalCampaignMap.Campaign.values())counts.put(c,0);
        for(int i=0;i<144;i++){
            ChemCoreExperimentalCampaignMap.Campaign c=ChemCoreExperimentalCampaignMap.campaignForCatalogIndex(i);
            assertNotNull(c);counts.put(c,counts.get(c)+1);
        }
        assertEquals(Integer.valueOf(4),counts.get(ChemCoreExperimentalCampaignMap.Campaign.TARGET_ANCHOR));
        assertEquals(Integer.valueOf(97),counts.get(ChemCoreExperimentalCampaignMap.Campaign.BROAD_ARRAY_PURE));
        assertEquals(Integer.valueOf(9),counts.get(ChemCoreExperimentalCampaignMap.Campaign.MIXTURE_OR_AGGREGATE));
        assertEquals(Integer.valueOf(19),counts.get(ChemCoreExperimentalCampaignMap.Campaign.COMPATIBILITY_REVIEW));
        assertEquals(Integer.valueOf(15),counts.get(ChemCoreExperimentalCampaignMap.Campaign.HARDWARE_GAP));
    }
}
