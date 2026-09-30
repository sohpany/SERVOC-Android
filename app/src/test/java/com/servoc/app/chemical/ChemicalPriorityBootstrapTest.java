package com.servoc.app.chemical;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ChemicalPriorityBootstrapTest {
    @Test public void acceptedIdentityFingerprintPromotes(){
        assertTrue(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED_IDENTITY_FINGERPRINT","PEER_REVIEWED_SAME_MQ_IDENTITY_FINGERPRINT",null,null,null));
    }

    @Test public void acceptedPpmCurvePromotes(){
        assertTrue(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED_IDENTITY_AND_PPM_REFERENCE","PEER_REVIEWED_RS_R0_PPM_CURVE",20.0,2000.0,null));
    }

    @Test public void quantitativeValidationPromotes(){
        assertTrue(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED_IDENTITY_AND_QUANTITATIVE_VALIDATION_POINTS","PUBLISHED_MQ135_NO2_THIRD_PARTY_PPM_VALIDATION",0.62,10.0,null));
    }

    @Test public void feasibilityAndAwaitingRawDoNotPromote(){
        assertFalse(ChemicalPriorityBootstrap.qualifies(
                "REFERENCE_ONLY_NO_DIRECT_HR069_ID_OR_PPM","MODIFIED_MQ135_CHLORINE_FEASIBILITY_EXPERIMENT",null,null,null));
        assertFalse(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED_SOURCE_DESCRIPTOR_AWAITING_RAW","EXTERNAL_SAME_ARRAY_METHANOL_FINGERPRINT_DESCRIPTOR",null,null,null));
    }

    @Test public void familyOnlyEvidenceDoesNotPromoteNewIdentity(){
        assertFalse(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED_FAMILY_LABEL_AND_PPM_SERIES","PEER_REVIEWED_7CHANNEL_VOC_10_100PPM_SERIES",10.0,100.0,null));
    }

    @Test public void explicitAcceptedPayloadPromotesEvenWhenStatusIsGenericAccepted(){
        assertTrue(ChemicalPriorityBootstrap.qualifies(
                "ACCEPTED","REFERENCE_RESPONSE_MODEL",null,null,"{\"identity_fingerprint_accepted\":true}"));
    }
}
