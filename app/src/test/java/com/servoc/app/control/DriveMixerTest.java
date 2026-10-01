package com.servoc.app.control;

import org.junit.Test;
import static org.junit.Assert.*;

public class DriveMixerTest {
    private static void assertMix(double x,double y,int left,int right){
        DriveMixer.Command c=DriveMixer.mix(x,y);
        assertEquals(left,c.left);assertEquals(right,c.right);
    }

    @Test public void cardinalTravelAndPivotArePredictable(){
        assertMix(0,0,0,0);
        assertMix(0,1,100,100);
        assertMix(0,-1,-100,-100);
        assertMix(1,0,65,-65);
        assertMix(-1,0,-65,65);
    }

    @Test public void forwardRightSlowsLeftSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(0.5,0.8,56,80);
        assertMix(1.0,0.8,32,80);
    }

    @Test public void forwardLeftSlowsRightSideProgressively(){
        assertMix(0.0,0.8,80,80);
        assertMix(-0.5,0.8,80,56);
        assertMix(-1.0,0.8,80,32);
    }

    @Test public void fullSteeringLockKeepsSlowedSideAtFortyPercent(){
        DriveMixer.Command right=DriveMixer.mix(1,1);
        DriveMixer.Command left=DriveMixer.mix(-1,1);
        assertEquals(40,right.left);assertEquals(100,right.right);
        assertEquals(100,left.left);assertEquals(40,left.right);
        assertEquals(DriveMixer.MIN_INNER_RATIO,Math.abs(right.left/(double)right.right),0.001);
    }

    @Test public void reverseUsesPathRelativeSteeringRule(){
        assertMix(1.0,-0.8,-80,-32);
        assertMix(-1.0,-0.8,-32,-80);
    }

    @Test public void pureHorizontalInputIsLimitedToSixtyFivePercentPivot(){
        assertMix(0.5,0,33,-33);
        assertMix(-0.5,0,-33,33);
        assertMix(2,0,65,-65);
    }

    @Test public void inputsAreBoundedToUnitCommandDomain(){
        assertMix(0,2,100,100);
        assertMix(0,-2,-100,-100);
        assertMix(2,2,40,100);
        assertMix(-2,2,100,40);
    }
}
