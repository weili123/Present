package com.group15.toq_o.present;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.group15.toq_o.present.Presentation.Presentation;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;

import java.util.ArrayList;


public class ViewFilesActivity extends Activity {

    ArrayList<Presentation> pptList;
    ListView list;
    PptListAdapter adapter;
    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteResourceStore mRemoteResourceStore;
    private ChangeSlidesListener mChangeSlidesListener = new ChangeSlidesListener(this);
    ListItemInfo current;
    RemoteDeckOfCards mRemoteDeckOfCards;

    private class ListItemInfo {
        int position;
        View view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);
        Toast complete = Toast.makeText(getBaseContext(), "sync complete", Toast.LENGTH_SHORT);
        complete.show();
        //get list of ppt + put into list
        PresentApplication app = (PresentApplication) getApplication();
        pptList = new ArrayList<Presentation>(app.getPresentationHashMap().values());
        current = null;
        Resources res = getResources();
        list = (ListView) findViewById(R.id.list);
        adapter = new PptListAdapter(this, pptList, res);
        list.setAdapter(adapter);
        //init watch objects
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        mDeckOfCardsManager.addDeckOfCardsEventListener(mChangeSlidesListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    public void onItemClick(int position, View view) {
        Presentation tempValues = pptList.get(position);
        ImageView radioButton = radioButton = (ImageView) view.findViewById(R.id.radio_button);
        if (current != null) {
            ImageView previous = (ImageView) current.view.findViewById(R.id.radio_button);
            previous.setImageResource(R.drawable.empty_radio);
        }
        current = new ListItemInfo();
        current.view = view;
        current.position = position;
        radioButton.setImageResource(R.drawable.full_radio);
        boolean canSync = tempValues.isCanSync();
        ImageButton button = (ImageButton) findViewById(R.id.watchButton);
        if (canSync) {
            button.setImageResource(R.drawable.active_next);
        } else {
            button.setImageResource(R.drawable.disable_next);
        }
    }

    public void downloadToWatch(View view) {
        int position = current.position;
        Presentation ppt = pptList.get(position);
        if (!ppt.isCanSync()) {
            return;
        }
        //install messages to watch
        mRemoteDeckOfCards = createDeckOfCards(pptList.get(position));
        try {
            if (!mDeckOfCardsManager.isInstalled()) {
                install(view);
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            }
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
        } catch(Exception e) {
            //should not fail
            e.printStackTrace();
        }
        //switch xml views
        //RelativeLayout layoutRight = (RelativeLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_sync_complete, null);
        setContentView(R.layout.activity_sync_complete);
    }

    public void install(View view) {
        boolean isInstalled = false;

        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            init();
            /*try {
                init();
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }*/
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try {
            Thread.sleep(5000);
            if(mDeckOfCardsManager.isInstalled()) {
                //startService(intent);
            } else {
                System.out.println("error installing deck");
            }
        } catch (Exception e) {
            System.out.println("error");
        }
    }

    private void init(){
        mRemoteResourceStore= new RemoteResourceStore();
    }
    //blue first, then green slide

    private RemoteDeckOfCards createDeckOfCards(Presentation ppt) {
        //create deck of cards based on slides and messages
        System.out.println("creating cards");
        ListCard listCard= new ListCard();
        int numSlides = ppt.numSlides();
        ppt.updatePosition(-1);
        SimpleTextCard card = new SimpleTextCard(ppt.getId());
        updateCardFromMessage(card, ppt.getId(), -1, "");
        if (!card.isReceivingEvents()) {
            card.setReceivingEvents(true);
        }
        listCard.add(card);
        //addStartingSimpleTextCard("jack_weinberg_toq.png", "Jack Weinberg", "Draw Text: FSM", listCard);
        return new RemoteDeckOfCards(this, listCard);
    }

    private void updateCardFromMessage(SimpleTextCard card, String id, int slideNum, String message) {
        //Create a SimpleTextCard with id being pptId_slideNum
        if (card == null) {
            return;
        }
        String[] messages = new String[2];
        if (slideNum == -1) {
            messages[0] = "Start";
        }
        if (slideNum == 0) {
            messages[0] = "Introduction";
            messages[1] = "1/3";
        }
        if (slideNum == 1) {
            messages[0] = "Pre-attentive retain";
            messages[1] = "2/3";
        }
        if (slideNum == 2) {
            messages[0] = "Maintenance Rehearsal";
            messages[1] = "3/3";
        }
        //messages[0] = String.valueOf(slideNum);
        card.setMessageText(messages);
        Presentation ppt = pptList.get(current.position);
        String[] menuOption;
        if (slideNum == -1) {
            menuOption = new String[1];
            menuOption[0] = "Begin";
        } else if (slideNum == 0) {
            menuOption = new String[1];
            menuOption[0] = "Next";
        } else if (slideNum == ppt.numSlides() -1) {
            menuOption = new String[1];
            menuOption[0] = "Previous";
        } else {
            menuOption = new String[2];
            menuOption[0] = "Next";
            menuOption[1] = "Previous";
        }
        card.setMenuOptions(menuOption);
        card.setReceivingEvents(true);
    }

    void updateCard(String id, String menu) {
        ListCard listCard = mRemoteDeckOfCards.getListCard();
        SimpleTextCard card = (SimpleTextCard) listCard.get(0);
        Presentation ppt = pptList.get(current.position);
        int pos = ppt.getPosition();
        if (menu.equals("Previous")) {
            //go to previous slide
            int slideNumber = pos - 1;
            if (slideNumber >= 0) {
                updateCardFromMessage(card, id, slideNumber, String.valueOf(slideNumber));
                ppt.updatePosition(slideNumber);
            }
        } else {
            //go to next slide
            int slideNumber = pos + 1;
            System.out.println(slideNumber);
            if (slideNumber < ppt.numSlides()) {
                updateCardFromMessage(card, id, slideNumber, String.valueOf(slideNumber));
                ppt.updatePosition(slideNumber);
            }
        }
        try {
            mDeckOfCardsManager.updateDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
        } catch (RemoteDeckOfCardsException e) {
            //shouldn't reach this
        }
    }

    void updateImage(String id, String menu) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView img = (ImageView) findViewById(R.id.image);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                int position = pptList.get(current.position).getPosition();
                if(position == 0) {
                    img.setImageResource(R.drawable.human_models);
                }
                if(position == 1) {
                    img.setImageResource(R.drawable.slide2);
                }
                if(position == 2) {
                    img.setImageResource(R.drawable.slide1);
                }
            }
        });
    }

    /*private void addStartingSimpleTextCard(String filename, String name, String drawing, ListCard listCard) {
        int currSize = listCard.size();

        // Create a SimpleTextCard with 1 + the current number of SimpleTextCards
        SimpleTextCard simpleTextCard = new SimpleTextCard(Integer.toString(currSize+1));

        //add image to card
        CardImage image = null;
        try {
            image = new CardImage(filename, getBitmap(filename));
        } catch (Exception e) {
            System.out.println(filename + " is not available");
            e.printStackTrace();
        }
        mRemoteResourceStore.addResource(image);
        simpleTextCard.setCardImage(mRemoteResourceStore, image);

        //add message to card
        String[] messages = new String[2];
        messages[0] = name;
        messages[1] = drawing;
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(false);
        simpleTextCard.setShowDivider(true);

        listCard.add(simpleTextCard);
    }*/
}
