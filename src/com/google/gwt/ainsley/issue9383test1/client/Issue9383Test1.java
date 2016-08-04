package com.google.gwt.ainsley.issue9383test1.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Issue9383Test1 implements EntryPoint {

   // Use this instead of generating random (to remove the cost of randomisation from test)
   
   private static final int DATA_BUFFER_SIZE = 4096;
   
   static char BUFFER[] = new char[DATA_BUFFER_SIZE];
   
   private static final int[] REPRESENTATIVE_PARAMETERS = new int[] {
      
      // NOTE :: These values should always be less than the size of the buffer .... 
      
      4, 7, 2, 12,  20, 24, 1, 37, 4,  7,
      9, 5, 9, 210, 27,  5, 4,  8, 17, 22 
   };
   
   private static final int REPRESENTATIVE_PARAMETERS_SIZE = REPRESENTATIVE_PARAMETERS.length;
   
   static {
      // Set up the buffer with some ababababababa.... values
      for (int i=0; i < DATA_BUFFER_SIZE; i++) {
         BUFFER[i] = (i % 2 == 0) ? 'a' : 'b';
      }
   }
   
   /**
    * This is the entry point method.
    */
   public void onModuleLoad() {
  
      // NOTE :: CBA -- This project is based on the template project therefore
      
      final Button sendButton = new Button("Execute");
      final TextBox nameField = new TextBox();
      nameField.setText("200000");
      final Label errorLabel = new Label();

      // We can add style names to widgets
      sendButton.addStyleName("sendButton");

      // Add the nameField and sendButton to the RootPanel
      // Use RootPanel.get() to get the entire body element
      RootPanel.get("nameFieldContainer").add(nameField);
      RootPanel.get("sendButtonContainer").add(sendButton);
      RootPanel.get("errorLabelContainer").add(errorLabel);

      // Focus the cursor on the name field when the app loads
      nameField.setFocus(true);
      nameField.selectAll();

      // Create the popup dialog box
      final DialogBox dialogBox = new DialogBox();
      dialogBox.setText("Remote Procedure Call");
      dialogBox.setAnimationEnabled(true);
      final Button closeButton = new Button("Close");
      // We can set the id of a widget by accessing its Element
      closeButton.getElement().setId("closeButton");
      final Label textToServerLabel = new Label();
      final HTML serverResponseLabel = new HTML();
      final Label callsPerMillisecond = new Label("");
      VerticalPanel dialogVPanel = new VerticalPanel();
      dialogVPanel.addStyleName("dialogVPanel");
      dialogVPanel.add(new HTML("<b>Buffer Size:</b>"));
      dialogVPanel.add(new Label(String.valueOf(DATA_BUFFER_SIZE)));
      dialogVPanel.add(new HTML("<b>GWT Version:</b>"));
      dialogVPanel.add(new Label(GWT.getVersion()));
      dialogVPanel.add(new HTML("<b>Number of Iterations:</b>"));
      dialogVPanel.add(textToServerLabel);
      dialogVPanel.add(new HTML("<br><b>Time Taken:</b>"));
      dialogVPanel.add(serverResponseLabel);
      dialogVPanel.add(new HTML("<br><b>Calls Per Millisecond:</b>"));
      dialogVPanel.add(callsPerMillisecond);
      
      dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
      dialogVPanel.add(closeButton);
      dialogBox.setWidget(dialogVPanel);

      // Add a handler to close the DialogBox
      closeButton.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event) {
            dialogBox.hide();
            sendButton.setEnabled(true);
            sendButton.setFocus(true);
         }
      });

      // Create a handler for the sendButton and nameField
      class MyHandler implements ClickHandler, KeyUpHandler {
         /**
          * Fired when the user clicks on the sendButton.
          */
         public void onClick(ClickEvent event) {
            onSubmit();
         }

         /**
          * Fired when the user types in the nameField.
          */
         public void onKeyUp(KeyUpEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
               onSubmit();
            }
         }

         /**
          * Send the name from the nameField to the server and wait for a response.
          */
         private void onSubmit() {
            // First, we validate the input.
            errorLabel.setText("");
            
            String inputText = nameField.getText();
            long numberOfIterations = 0;
            
            // Validate input between 1 and 1000000
            {
               
               boolean valid = true;
               try {
                  numberOfIterations = Long.parseLong(inputText);
                  if (numberOfIterations < 1 || numberOfIterations > 500000) {
                     valid = false;
                  }
               } catch (Exception e) {
                  valid = false;
               }
               
               if (!valid) {
                  errorLabel.setText("Please provide an integer value between 1 and 500000 (half a million).");
                  return;
               }
            }
            // Now perform the test
            sendButton.setEnabled(false);
            textToServerLabel.setText(inputText);
            serverResponseLabel.setText("");
            
            // NOTE :: This will hang the browser for some time
            
            long timeTaken = performTest(numberOfIterations, 4096 /* data buffer size */);
            
            boolean success = true;
            
            if (success) {
               dialogBox.setText("String.valueOf(char[] data, int offset, int count) test");
               serverResponseLabel.removeStyleName("serverResponseLabelError");
               serverResponseLabel.setHTML(timeTaken + " millisecond" + (timeTaken == 1 ? "" : "s"));
               if (timeTaken > 0) {
                  final long callsPerMilli = numberOfIterations / timeTaken;
                  callsPerMillisecond.setText(String.valueOf(callsPerMilli) + " call"+(callsPerMilli == 1 ? "" : "s") +" per milliseond.");
               } else {
                  callsPerMillisecond.setText("N/A");
               }
               dialogBox.center();
               closeButton.setFocus(true);
            }
         }
      }

      // Add a handler to send the name to the server
      MyHandler handler = new MyHandler();
      sendButton.addClickHandler(handler);
      nameField.addKeyUpHandler(handler);
   }
   

   
   private long performTest(long numberOfIterations, int databufferSize) {
      
      long startTime = System.currentTimeMillis();

      long fauxCount = 0; // Use this to make sure the optimizer doesn't remove the String.valueOf() call
      
      for (int i=0; i < numberOfIterations; i++) {
         
         // Cycles over 20 possible values
         int currentCount = REPRESENTATIVE_PARAMETERS[i % REPRESENTATIVE_PARAMETERS_SIZE];
         
         // We are safe with the default static parameters,
         // but if we want to be more flexible, then this needs further bounds checking.
         
         final String string = String.valueOf(BUFFER, currentCount /* start offset */, currentCount /* length */);
         
         // Doing something with the result to avoid the optimizer not performing the String.valueOf() operation
         fauxCount+= string.length();
         
         if (fauxCount > 1000000) {
            fauxCount -= 1000000;
         }
      }
      
      System.out.println(fauxCount);
      
      long duration = System.currentTimeMillis() - startTime;
      
      return duration;
   }
}
