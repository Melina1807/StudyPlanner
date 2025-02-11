package Studyplaner;

import static Helper.LocalDateTimeConverter.convertEventToEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;

import DataAccess.EventUpdateDB;
import DataAccess.EventsDeleteDB;
import DataAccess.LoadEventDB;
import DataAccess.LoadModulDDB;
import Model.Event;
import Model.Modul;
import View.ButtonAndElement;
import View.EditandDeleteModul;
import View.Helper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * The class Study planner.
 */
public class StudyPlanner extends Application {

    static final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("StudyPlanner");
    static final EntityManager entityManager = entityManagerFactory.createEntityManager();
    static final EntityTransaction entityTransaction = entityManager.getTransaction();
    /**
     * The Module.
     */
    final List<Modul> Module = new ArrayList<>();
    /**
     * The Events.
     */
    final List<Event> Events = new ArrayList<>();
    /**
     * The School time table.
     */
    final Calendar SchoolTimeTable = new Calendar("Stundenplan");
    /**
     * The Study plan.
     */
    final Calendar StudyPlan = new Calendar("Lernplan");
    final ButtonAndElement buttonAndElement = new ButtonAndElement();
    final Helper helper = new Helper();
    /**
     * The Listbox.
     */
    final ListView<Button> listbox = new ListView<>();

    /**
     * The entry point of application.
     *
     * @param args
     *             the input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


    /**
     * start Method to set the stage
     * @param stage
     */
    @Override
    public void start(Stage stage) {

        stage.setMinWidth(1024);
        stage.setHeight(768);
        stage.centerOnScreen();
        stage.setTitle("Study Planer");
        stage.show();

        Platform.runLater(() -> {

            CalendarView calendarView = new CalendarView();

            calendarEventHandler();

            SchoolTimeTable.setStyle(Style.STYLE2);
            StudyPlan.setStyle(Style.STYLE3);

            Platform.runLater(() -> {

                /*
                 * @Marc Load Data from DB and add Module to listbox
                 */
                LoadModulDDB loadModulDDB = new LoadModulDDB();
                for (Modul modul : loadModulDDB.zeigemodul(entityManager, entityTransaction)) {
                    Module.add(modul);
                    modul.setEcts(modul.gettEcts());
                    Button bt = new Button(modul.toString2());
                    listbox.getItems().add(bt);
                    EditandDeleteModul editandDeleteModul = new EditandDeleteModul();
                    bt.setOnAction(actionEvent -> editandDeleteModul.editModul(modul, bt, entityManager,
                            entityTransaction, Module, listbox, Events, SchoolTimeTable, StudyPlan));

                }
                LoadEventDB loadEventDB = new LoadEventDB();
                for (Event event : loadEventDB.zeigeEvent(entityManager, entityTransaction)) {
                    Events.add(event);
                    Entry<?> entry = convertEventToEntry(event);
                    if (Objects.equals(event.getCalendar(), "Stundenplan")) {
                        SchoolTimeTable.addEntry(entry);
                    } else if (Objects.equals(event.getCalendar(), "Lernplan")) {
                        StudyPlan.addEntry(entry);
                    }

                }
            });

            helper.initializingCalenderView(calendarView, StudyPlan, SchoolTimeTable);

            Button BtCreateEvent = buttonAndElement.getBtCreateEvent(Module, Events, StudyPlan, SchoolTimeTable,
                    entityManager, entityTransaction);
            Button BtCreateModul = buttonAndElement.getBtCreateModul(Module, listbox, entityManager, entityTransaction,
                    Events, SchoolTimeTable, StudyPlan);
            Button BtDeleteModul = buttonAndElement.getBtDeleteModul(Module, Events, SchoolTimeTable, StudyPlan,
                    entityManager, entityTransaction, listbox);
            Button BtShowQuote = buttonAndElement.getBtShowQuote();
            Button BtICalExport = buttonAndElement.getBtICalExport(Events);
            Pane leftSideSplitPane = buttonAndElement.getLeftSideSplitPane(BtCreateEvent, BtCreateModul, BtDeleteModul,
                    listbox, BtShowQuote, BtICalExport);
            listbox.setMaxWidth(200);

            SplitPane split = new SplitPane(leftSideSplitPane, calendarView);
            leftSideSplitPane.setMaxWidth(200);
            Scene sceneO = new Scene(split);
            stage.setScene(sceneO);
            sceneO.getStylesheets().add(getClass().getResource("Application.css").toExternalForm());

        });

    }

    /**
     * Calendar event handler.
     * <p>
     * Adding the EventHandler to the Calendar´s
     *
     * @author Andreas Scheuer
     */
    public void calendarEventHandler() {
        Platform.runLater(() -> {

            // Eventhandler für alle arten von Events
            StudyPlan.addEventHandler(event -> {
                // ToDo: es ist kein check eingebaut falls der Eintrag den Kalender wechselt,
                // muss noch eingebaut werden
                // check added -> Works without description, need to be changed. Module gets the
                // uuid
                isEntryAdded(event);
                // check removed-> Works without description, need to be changed. Module gets
                // the uuid
                isEntryRemoved(event);
                // Title changed
                isEntryTitleChanged(event);
                // Intervall changed, works fine
                isEntryIntervallChanged(event);
            });
        });

        SchoolTimeTable.addEventHandler(event -> {
            Platform.runLater(() -> {
                // check added -> Works without description, need to be changed. Module gets the
                // uuid
                isEntryAdded(event);
                // check removed-> Works without description, need to be changed. Module gets
                // the uuid
                isEntryRemoved(event);
                // Title changed
                isEntryTitleChanged(event);
                // Intervall changed, works fine
                isEntryIntervallChanged(event);
            });
        });
    }

    /**
     * Is entry added method to add the event
     *
     * @param event
     *              the event
     */
    public void isEntryAdded(CalendarEvent event) {
        if (event.isEntryAdded()) {
            for (Modul modul : Module) {
                if (modul.getUuid().contains(event.getEntry().getId())) {
                    modul.getEcts().setDuration(modul.getEcts().getDuration().minus(event.getEntry().getDuration()));

                    helper.changeListBoxButtonText(modul, listbox);
                }
            }
        }
    }

    /**
     * Is entry removed, to remove the event
     *
     * @param event
     *              the event
     */
    public void isEntryRemoved(CalendarEvent event) {
        if (event.isEntryRemoved()) {
            for (Modul modul : Module) {
                if (modul.getUuid().contains(event.getEntry().getId())) {
                    modul.getEcts().setDuration(modul.getEcts().getDuration().plus(event.getEntry().getDuration()));

                    helper.changeListBoxButtonText(modul, listbox);

                    /*
                     * removes the Events from the Eventlist
                     */
                    List<Event> events = Events.stream().filter(e -> e.getId().equals(event.getEntry().getId()))
                            .collect(Collectors.toList());
                    EventsDeleteDB eventsDeleteDB = new EventsDeleteDB();
                    eventsDeleteDB.EventDelete(events, entityManager, entityTransaction);
                    Events.removeAll(events);

                }
            }

        }
    }

    /**
     * Is entry title changed, to change the title of the event
     *
     * @param event
     *              the event
     */
    public void isEntryTitleChanged(CalendarEvent event) {
        if (!event.isEntryAdded() && !event.isEntryRemoved() && event.getOldInterval() == null
                && !event.getOldText().equals(event.getEntry().getTitle())) {

            // ToDo: hier stimmt noch was nicht der Titel wird nicht geändert aber bei
            // Intervall Changed klappt es warum auch immer
            EventUpdateDB eventUpdateDB = new EventUpdateDB();

            Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
                e.setTitle(event.getEntry().getTitle());

                eventUpdateDB.updateEvent(e, entityManager, entityTransaction);

            });
        }
    }

    /**
     * Check´s if the Entry Intervall is Changed and if so it Updates the Event with
     * the same UUID
     *
     * @param event
     *              the event
     */
    public void isEntryIntervallChanged(CalendarEvent event) {

        // Update Event aus der Datenbank 1
        EventUpdateDB eventUpdateDB = new EventUpdateDB();
        if (!event.isEntryAdded() && !event.isEntryRemoved() && !(event.getOldInterval() == null)
                && !event.getOldInterval().getDuration().equals(event.getEntry().getInterval().getDuration())) {
            Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
                e.setStartTime(event.getEntry().getStartTime().toString());
                e.setStarDate(event.getEntry().getStartDate().toString());
                e.setEndTime(event.getEntry().getEndTime().toString());
                e.setEndDate(event.getEntry().getEndDate().toString());
                eventUpdateDB.updateEvent(e, entityManager, entityTransaction);
            });

            Module.stream().filter(e -> e.getUuid().contains(event.getEntry().getId())).forEach(e -> {
                e.getEcts().setDuration(e.getEcts().getDuration()
                        .plus(event.getOldInterval().getDuration().minus(event.getEntry().getDuration())));

                helper.changeListBoxButtonText(e, listbox);
            });
        }
    }
}
