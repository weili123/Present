package com.group15.toq_o.present;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.group15.toq_o.present.Presentation.Presentation;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;

import java.util.ArrayList;


public class ViewFilesActivity extends Activity {

    ArrayList<Presentation> pptList;
    ListView list;
    PptListAdapter adapter;
    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private ToqBroadcastReceiver toqReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);
        Toast complete = Toast.makeText(getBaseContext(), "sync complete", Toast.LENGTH_SHORT);
        complete.show();
        //get list of ppt + put into list
        PresentApplication app = (PresentApplication) getApplication();
        pptList = new ArrayList<Presentation>(app.getPresentationHashMap().values());
        Resources res = getResources();
        list = (ListView) findViewById(R.id.list);
        adapter = new PptListAdapter(this, pptList, res);
        list.setAdapter(adapter);
        //init watch objects
        toqReceiver = new ToqBroadcastReceiver();
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new ToqBroadcastReceiver();
    }

    @Override
    protected void onStart() {
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    public void onItemClick(int position) {
        Presentation tempValues = pptList.get(position);
        System.out.println(tempValues.getName() + " " + position);
    }

    public void onInstall(View view) {
        boolean isInstalled = false;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            try {
                init();
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try {
            Thread.sleep(5000);
            if(mDeckOfCardsManager.isInstalled()) {
                Intent intent = new Intent(getApplicationContext(),
                        LocatorService.class);
                startService(intent);
            } else {
                System.out.println("error installing deck");
            }
        } catch (Exception e) {
            System.out.println("error");
        }
    }

    private void init(){

        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();

        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;

        // Get the launcher icons
        try{
            whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            mRemoteDeckOfCards = createDeckOfCards();
        }
        catch (Throwable th){
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null){
            mRemoteDeckOfCards = createDeckOfCards();
        }

        // Set the custom launcher icons, adding them to the resource store
        mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});
    }

    private RemoteDeckOfCards createDeckOfCards() {
        //create deck of cards based on slides and messages
        System.out.println("creating cards");
        ListCard listCard= new ListCard();
        /*addStartingSimpleTextCard("jack_weinberg_toq.png", "Jack Weinberg", "Draw Text: FSM", listCard);
        addStartingSimpleTextCard("joan_baez_toq.png", "Joan Baez", "Draw Megaphone", listCard);
        addStartingSimpleTextCard("michael_rossman_toq.png", "Michael Rossman", "Draw Text: Free Speech", listCard);
        addStartingSimpleTextCard("art_goldberg_toq.png", "Art Goldberg", "Draw Text: Now", listCard);
        addStartingSimpleTextCard("jackie_goldberg_toq.png", "Jackie Goldberg", "Draw Text: SLATE", listCard);
        addStartingSimpleTextCard("mario_savio_toq.png", "Mario Savio", "Draw FSM", listCard);*/
        return new RemoteDeckOfCards(this, listCard);
    }

    /*private void addStartingSimpleTextCard(String filename, String name, String drawing, ListCard listCard) {
        int currSize = listCard.size();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

        CardImage image = null;
        try {
            image = new CardImage(filename, getBitmap(filename));
        } catch (Exception e) {
            System.out.println(filename + " is not available");
            e.printStackTrace();
        }
        mRemoteResourceStore.addResource(image);
        simpleTextCard.setCardImage(mRemoteResourceStore, image);
        String[] messages = new String[2];
        messages[0] = name;
        messages[1] = drawing;
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        simpleTextCard.setShowDivider(true);

        listCard.add(simpleTextCard);
    }*/
}
