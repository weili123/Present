package com.group15.toq_o.present;

import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;

public class ChangeSlidesListener implements DeckOfCardsEventListener {

    ViewFilesActivity activity;

    ChangeSlidesListener (ViewFilesActivity activity) {
        this.activity = activity;
    }


    @Override
    public void onCardOpen(String s) {
        //no need to use this
        //should switch ppt image on phone, right now just print something to show that this registered
        System.out.println("opened");
    }

    @Override
    public void onCardVisible(String s) {
    }

    @Override
    public void onCardInvisible(String s) {
        //no need to use this
    }

    @Override
    public void onCardClosed(String s) {
        //no need to use this
    }

    @Override
    public void onMenuOptionSelected(String s, String s2) {
        //no need to use this
        activity.updateCard(s, s2);
        activity.updateImage(s, s2);
    }

    @Override
    public void onMenuOptionSelected(String s, String s2, String s3) {
        System.out.println("selected");
    }
}
