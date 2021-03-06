package processing.editor;

import java.util.List;
import mapmaker.general.Mode;
import mapmaker.entities.*;
import mapmaker.entities.sprites.*;
import processing.ProcessingMapmaker;
import static processing.core.PApplet.dist;
import processing.core.PGraphics;
import processing.entities.*;
import processing.entities.sprites.*;
import processing.map.CanvasP3;
import processing.map.RouteLoaderP3;
import processing.map.ui.*;

/**
 *
 * @author TeamOne
 */
public class EditorP3 implements IEditorP3 {

    private final ProcessingMapmaker app;
    private final ModeUI_P3 modeUI;
    private final InfoUI_P3 infoUI;
    private final SpriteUI_P3 locationUI;
    private final CanvasP3 canvas;

    private int layer = 0;
    private LocationP3 selectedLocation = null;

    private final int[] colors;
    private final float borderThickness = 10;
    private int selectedBrushColor = 1;

//    private int active = 0;
    public EditorP3(CanvasP3 canvas, ModeUI_P3 modeUI, InfoUI_P3 infoUI, ProcessingMapmaker app) {
        this.app = app;
        this.modeUI = modeUI;
        this.infoUI = infoUI;
        this.locationUI = new SpriteUI_P3("sprites/locations/", app);
        this.canvas = canvas;
        this.colors = new int[]{
            app.color(58, 168, 214),
            app.color(128, 199, 228),
            app.color(197, 229, 243),
            app.color(135, 209, 63),
            app.color(203, 227, 107),
            app.color(255, 227, 153),
            app.color(234, 136, 70),
            app.color(209, 104, 47),
            app.color(187, 76, 15),
            app.color(148, 56, 0)
        };
    }

    @Override
    public void displayUI() {
        modeUI.display();
        if (modeUI.isCurrentMode(Mode.MARKER)) {
            locationUI.display();
            viewLocationInfo(selectedLocation);
        }
        if (modeUI.isCurrentMode(Mode.LANDMASS, Mode.ROUTE)) {
            infoUI.loadLayerUIGraphics(layer);
        }
        if (modeUI.isCurrentMode(Mode.WATER)) {
            if (layer == canvas.getCurrentMap().getRegions().size()) {
                infoUI.loadLayerUIGraphics(-1);
            } else {
                infoUI.loadLayerUIGraphics(layer);
            }
        }
        if (modeUI.isCurrentMode(Mode.LANDMASS)) {
            loadColorUIGraphics();
        }
        if (modeUI.isCurrentMode(Mode.LANDMASS, Mode.WATER)) {
            viewRegion(canvas.getCurrentMap().getRegion(layer));
        }
    }

    @Override
    public void draw(int x, int y) {
//        PShape circle = app.createShape(ELLIPSE, x, y, 100, 100);
//        circle.setFill(app.color(colors[selectedBrushColor]));
//        app.shape(circle);

        // TEMPORARY EFFECT (to demonstrate Proxy)
        app.noFill();
        app.ellipse(x, y, 10, 10);
    }

    @Override
    public void drawLandmass(int x, int y, double radius, Region optionalRegion) {
        if (canvas.getCurrentMap().getRegions().size() < layer) {
            layer = 0;
        }
        float fRadius = (float) radius;
        if (app.mousePressed) {
            if (optionalRegion == null) {
                createNewRegionLayer();
            }
            final Region region = canvas.getCurrentMap().getRegion(layer);
            if (region != null) {
                final LandmassP3 area = (LandmassP3) region.getArea();
                final PGraphics land = area.getGraphics();
                final PGraphics border = area.getBorderGraphics();

                land.beginDraw();
                land.stroke(colors[selectedBrushColor]);
                land.strokeWeight(fRadius - borderThickness);
                land.line(app.pmouseX, app.pmouseY, x, y);
                land.endDraw();

                border.beginDraw();
                border.stroke(0);
                border.strokeWeight(fRadius);
                border.line(app.pmouseX, app.pmouseY, x, y);
                border.endDraw();
            }
        }
    }

    @Override
    public void drawWater(int x, int y, double radius) {
        canvas.getCurrentMap().getRegions().forEach((region) -> {
            deleteLandmassFromRegion(x, y, radius, region);
        });
    }

    @Override
    public void deleteLandmassFromRegion(int x, int y, double radius, Region region) {
        float fRadius = (float) radius;
        if (app.mousePressed) {
            final LandmassP3 area = (LandmassP3) region.getArea();
            final PGraphics land = area.getGraphics();
            final PGraphics border = area.getBorderGraphics();
            border.beginDraw();
            border.loadPixels();
            land.beginDraw();
            land.loadPixels();
            for (int px = 0; px < land.width; px++) {
                for (int py = 0; py < land.height; py++) {
                    float distance = dist(px, py, x, y);
                    if (distance <= fRadius * 0.5) {
                        int loc = px + py * land.width;
                        land.pixels[loc] = 0x0;
                    }
                    if (distance <= (fRadius - borderThickness) * 0.5f) {
                        int loc = px + py * land.width;
                        border.pixels[loc] = 0x0;
                    }
                }
            }
            land.updatePixels();
            land.endDraw();
            border.updatePixels();
            border.endDraw();
        }
        app.noStroke();
        app.ellipse(x, y, fRadius, fRadius);
    }

    @Override
    public void createNewRegionLayer() {
        canvas.getCurrentMap().addRegion(new Region(
                new LandmassP3(app.createGraphics(app.width, app.height), app.createGraphics(app.width, app.height)),
                new EntityInfo("New Region"))
        );
        System.out.println("New REGION added");
    }

    @Override
    public List<Region> updateRegions() {
        System.out.println("Regions updated.");
        return canvas.getCurrentMap().getRegions();
    }

    @Override
    public void viewRegion(Region region) {
        if (region != null) {
            infoUI.loadInfoUIGraphics(region.getInfo());
        }
    }

    @Override
    public void editRegion(Region original, EntityInfo updatedInfo) {
        original.setInfo(updatedInfo);
    }

    @Override
    public void drawRoute(int x, int y, double radius, Route optionalRoute) {
        if (optionalRoute == null) {
            createNewRouteLayer();
        }
        RouteP3 route = (RouteP3) canvas.getCurrentMap().getRoutes().get(layer);
        route.addPoint(x, y);
    }

    @Override
    public void eraseRoute(int x, int y, double radius, Route route) {
        RouteP3 routeP3 = (RouteP3) route;
        routeP3.getPoints().remove(routeP3.getPoints().size() - 1);
    }

    @Override
    public void createNewRouteLayer() {
        canvas.getCurrentMap().addRoute(new RouteP3(
                app.createGraphics(app.width, app.height))
        );
        System.out.println("New ROUTE added");
    }

    @Override
    public Route updateRoute() {
        RouteP3 route = (RouteP3) canvas.getCurrentMap().getRoutes().get(layer);
        RouteLoaderP3.loadRoute(route);
        System.out.println("Route updated.");
        return route;
    }

    @Override
    public LocationP3 getSelectedLocation() {
        return selectedLocation;
    }

    @Override
    public void attemptSelectLocation(int x, int y) {
        LocationP3 result = (LocationP3) locationUI.attemptSelectSpriteType();
        if (result == null) {
            for (int i = 0; i < canvas.getCurrentMap().getLocations().size(); i++) {
                LocationP3 locP3 = (LocationP3) canvas.getCurrentMap().getLocations().get(i);
                if (locP3.mouseIsOver(x, y)) {
                    result = locP3;
                    break;
                }
            }
        }
        selectedLocation = result;
    }

    @Override
    public void dragLocation(Location location, int x, int y) {
        LocationP3 locP3 = (LocationP3) location;
        if (locationUI.getSelectedSpriteType() != null) {
            app.image(locP3.getImage(), x, y, locP3.getWidth(), locP3.getHeight());
        }
        locP3.incrementCoordinates(x - app.pmouseX, y - app.pmouseY);
    }

    @Override
    public void dropLocation(Location location, int x, int y) {
        LocationP3 locP3 = (LocationP3) location;
        if (locationUI.getSelectedSpriteType() != null) {
            canvas.getCurrentMap().addLocation(locP3);
        }
        locationUI.selectSpriteType(null);
        selectedLocation = null;
    }

    @Override
    public void viewLocationInfo(Location location) {
        if (location != null) {
            infoUI.loadInfoUIGraphics(location.getInfo());
        }
    }

    @Override
    public void editLocationInfo(Location original, EntityInfo updatedInfo) {
        final List<Location> locations = canvas.getCurrentMap().getLocations();
        if (locations.contains(original)) {
            // DELETE LOCATION
            if (updatedInfo == null) {
                locations.remove(original);
                selectedLocation = null;
            } // EDIT LOCATION
            else {
                original.setInfo(updatedInfo);
            }
        }
    }

    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    public void setLayer(int layer) {
        if (layer == -1) {
            layer = canvas.getCurrentMap().getRegions().size();
        }
        this.layer = layer;
    }

    @Override
    public void switchRegionLayer() {
        layer = (layer < canvas.getCurrentMap().getRegions().size()) ? layer + 1 : 0;
        System.out.println("LANDMASS layer switch");
    }

    @Override
    public void switchRouteLayer() {
        layer = (layer < canvas.getCurrentMap().getRoutes().size()) ? layer + 1 : 0;
        System.out.println("ROUTE layer switch");
    }

    @Override
    public void switchColorBrush() {
        selectedBrushColor = (selectedBrushColor == colors.length - 1) ? 1 : selectedBrushColor + 1;
    }

    private void loadColorUIGraphics() {
        PGraphics uiLayer = app.createGraphics(app.width, app.height);
        uiLayer.beginDraw();
        uiLayer.fill(colors[selectedBrushColor]);
        uiLayer.rect(390, 10, 120, 50);
        uiLayer.fill(255);
        uiLayer.textSize(37);
        uiLayer.text("Color", 400, 50);
        uiLayer.endDraw();
        app.image(uiLayer, 0, 0);
    }
}
