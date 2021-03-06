/*******************************************************************************
 * Copyright 2016 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.wdc.geodev201611;

import com.esri.arcgisruntime.datasource.QueryParameters;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.mobilemappackage.MobileMapPackage;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.util.ListenableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * This Application class demonstrates key features of ArcGIS Runtime Quartz.
 */
public class WorkshopApp extends Application {
    
    // Exercise 1: Specify elevation service URL
    private static final String ELEVATION_IMAGE_SERVICE = 
            "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

    // Exercise 3: Specify mobile map package path
    private static final String MMPK_PATH = "../../../data/DC_Crime_Data.mmpk";
    
    // Exercise 4: Create symbols for click and buffer
    private static final SimpleMarkerSymbol CLICK_SYMBOL =
            new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFffa500, 10);
    private static final SimpleFillSymbol BUFFER_SYMBOL =
            new SimpleFillSymbol(SimpleFillSymbol.Style.NULL, 0xFFFFFFFF,
                    new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFFA500, 3));
    
    // Exercise 1: Declare and instantiate fields, including UI components
    private final MapView mapView = new MapView();
    private ArcGISMap map = new ArcGISMap();
    private final ImageView imageView_2d =
            new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/two_d.png")));
    private final ImageView imageView_3d =
            new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/three_d.png")));
    private final Button button_toggle2d3d = new Button(null, imageView_3d);
    private final AnchorPane anchorPane = new AnchorPane();
    private SceneView sceneView = null;
    private ArcGISScene scene = null;
    private boolean threeD = false;
    
    // Exercise 2: Declare UI components for zoom buttons
    private final ImageView imageView_zoomIn =
            new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/zoom_in.png")));
    private final ImageView imageView_zoomOut =
            new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/zoom_out.png")));
    private final Button button_zoomIn = new Button(null, imageView_zoomIn);
    private final Button button_zoomOut = new Button(null, imageView_zoomOut);
    
    // Exercise 4: Declare UI component for location button
    private final ImageView imageView_location =
            new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/location.png")));
    private final ToggleButton toggleButton_bufferAndQuery = new ToggleButton(null, imageView_location);
    
    // Exercise 4: Declare buffer and query fields
    private final GraphicsOverlay bufferAndQueryMapGraphics = new GraphicsOverlay();
    private final GraphicsOverlay bufferAndQuerySceneGraphics = new GraphicsOverlay();
    
    /**
     * Default constructor for class.
     */
    public WorkshopApp() {
        super();

        // Exercise 1: Set up the 2D map, since we will display that first
        map.setBasemap(Basemap.createNationalGeographic());
        mapView.setMap(map);

        // Exercise 1: Set the 2D/3D toggle button's action
        button_toggle2d3d.setOnAction(event -> button_toggle2d3d_onAction());
        
        // Exercise 2: Set the zoom buttons' actions
        button_zoomIn.setOnAction(event -> button_zoomIn_onAction());
        button_zoomOut.setOnAction(event -> button_zoomOut_onAction());
        
        /**
         * Exercise 3: Open a mobile map package (.mmpk) and
         * add its operational layers to the map
         */
        final MobileMapPackage mmpk = new MobileMapPackage(MMPK_PATH);
        mmpk.addDoneLoadingListener(() -> {
            List<ArcGISMap> maps = mmpk.getMaps();
            if (0 < maps.size()) {
                map = maps.get(0);
                mapView.setMap(map);
            }
            map.setBasemap(Basemap.createNationalGeographic());
        });
        mmpk.loadAsync();

        //Exercise 4: Add a GraphicsOverlay to the map for the click and buffer
        mapView.getGraphicsOverlays().add(bufferAndQueryMapGraphics);
        
        // Exercise 4: Set the buffer and query toggle button's action
        toggleButton_bufferAndQuery.setOnAction(event -> toggleButton_bufferAndQuery_onAction());
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Exercise 1: Place the MapView and 2D/3D toggle button in the UI
        AnchorPane.setLeftAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(mapView, 0.0);
        AnchorPane.setTopAnchor(mapView, 0.0);
        AnchorPane.setBottomAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(button_toggle2d3d, 15.0);
        AnchorPane.setBottomAnchor(button_toggle2d3d, 15.0);
        anchorPane.getChildren().addAll(mapView, button_toggle2d3d);

        // Exercise 2: Place the zoom buttons in the UI
        AnchorPane.setRightAnchor(button_zoomOut, 15.0);
        AnchorPane.setBottomAnchor(button_zoomOut, 80.0);
        AnchorPane.setRightAnchor(button_zoomIn, 15.0);
        AnchorPane.setBottomAnchor(button_zoomIn, 145.0);
        anchorPane.getChildren().addAll(button_zoomOut, button_zoomIn);
        
        // Exercise 4: Place the location button in the UI
        AnchorPane.setRightAnchor(toggleButton_bufferAndQuery, 90.0);
        AnchorPane.setBottomAnchor(toggleButton_bufferAndQuery, 15.0);
        anchorPane.getChildren().add(toggleButton_bufferAndQuery);
        
        // Exercise 1: Finish displaying the UI
        // JavaFX Scene (unrelated to ArcGIS 3D scene)
        Scene javaFxScene = new Scene(anchorPane);
        primaryStage.setTitle("My first map application");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setScene(javaFxScene);
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        // Exercise 1: Dispose of the MapView and SceneView before exiting
        mapView.dispose();
        if (null != sceneView) {
            sceneView.dispose();
        }
        
        super.stop();
    }

    /**
     * Exercise 1: Toggle between 2D map and 3D scene
     */
    private void button_toggle2d3d_onAction() {
        threeD = !threeD;
        button_toggle2d3d.setGraphic(threeD ? imageView_2d : imageView_3d);
        
        // Exercise 1: Switch between 2D map and 3D scene
        if (threeD) {
            if (null == scene) {
                // Set up the 3D scene. This only happens the first time the user switches to 3D.
                scene = new ArcGISScene();
                scene.setBasemap(Basemap.createImagery());
                Surface surface = new Surface();
                surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
                scene.setBaseSurface(surface);
                sceneView = new SceneView();
                
                /**
                 * Exercise 3: Open a mobile map package (.mmpk) and
                 * add its operational layers to the scene
                 */
                scene.addDoneLoadingListener(() -> {
                    final MobileMapPackage mmpk = new MobileMapPackage(MMPK_PATH);
                    mmpk.addDoneLoadingListener(() -> {
                        List<ArcGISMap> maps = mmpk.getMaps();
                        if (0 < maps.size()) {
                            final ArcGISMap thisMap = maps.get(0);
                            ArrayList<Layer> layers = new ArrayList<>();
                            layers.addAll(thisMap.getOperationalLayers());
                            thisMap.getOperationalLayers().clear();
                            scene.getOperationalLayers().addAll(layers);
                            sceneView.setViewpoint(thisMap.getInitialViewpoint());
                            // Rotate the camera
                            Viewpoint viewpoint = sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE);
                            Point targetPoint = (Point) viewpoint.getTargetGeometry();
                            Camera camera = sceneView.getCurrentViewpointCamera()
                                    .rotateAround(targetPoint, 45.0, 65.0, 0.0);
                            sceneView.setViewpointCameraAsync(camera);
                        }
                    });
                    mmpk.loadAsync();
                });
                
                /**
                 * Exercise 4: Add a GraphicsOverlay to the scene for the click
                 * and buffer.
                 */
                bufferAndQuerySceneGraphics.getSceneProperties().setSurfacePlacement(SurfacePlacement.DRAPED);
                sceneView.getGraphicsOverlays().add(bufferAndQuerySceneGraphics);
                
                /**
                 * Exercise 4: Set the SceneView's onMouseClicked event handler
                 * if the buffer and query button is already selected.
                 */
                if (toggleButton_bufferAndQuery.isSelected()) {
                    sceneView.setOnMouseClicked(event -> bufferAndQuery(event));
                }

                sceneView.setArcGISScene(scene);
                AnchorPane.setLeftAnchor(sceneView, 0.0);
                AnchorPane.setRightAnchor(sceneView, 0.0);
                AnchorPane.setTopAnchor(sceneView, 0.0);
                AnchorPane.setBottomAnchor(sceneView, 0.0);
            }
            anchorPane.getChildren().remove(mapView);
            anchorPane.getChildren().add(0, sceneView);
        } else {
            anchorPane.getChildren().remove(sceneView);
            anchorPane.getChildren().add(0, mapView);
        }
    }
    
    /**
     * Exercise 2: zoom in
     */
    private void button_zoomIn_onAction() {
        zoom(2.0);
    }
    
    /**
     * Exercise 2: zoom out
     */
    private void button_zoomOut_onAction() {
        zoom(0.5);
    }
    
    /**
     * Exercise 2: determine whether to call zoomMap or zoomScene
     */
    private void zoom(double factor) {
        if (threeD) {
            zoomScene(factor);
        } else {
            zoomMap(factor);
        }
    }
    
    /**
     * Exercise 2: Utility method for zooming the 2D map
     * @param factor the zoom factor (greater than 1 to zoom in, less than 1 to zoom out)
     */
    private void zoomMap(double factor) {
        mapView.setViewpointScaleAsync(mapView.getMapScale() / factor);
    }
    
    /**
     * Exercise 2: Utility method for zooming the 3D scene
     * @param factor the zoom factor (greater than 1 to zoom in, less than 1 to zoom out)
     */
    private void zoomScene(double factor) {
        Geometry target = sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getTargetGeometry();
        if (target instanceof Point) {
            Camera camera = sceneView.getCurrentViewpointCamera()
                    .zoomToward((Point) target, factor);
            sceneView.setViewpointCameraWithDurationAsync(camera, 0.5f);
        } else {
            Logger.getLogger(WorkshopApp.class.getName()).log(Level.WARNING,
                    "SceneView.getCurrentViewpoint returned {0} instead of {1}",
                    new String[] { target.getClass().getName(), Point.class.getName() });
        }
    }
    
    /**
     * Exercise 4: Activate buffer and query
     */
    private void toggleButton_bufferAndQuery_onAction() {
        if (toggleButton_bufferAndQuery.isSelected()) {
            mapView.setOnMouseClicked(mouseEvent -> bufferAndQuery(mouseEvent));
            if (null != sceneView) {
                sceneView.setOnMouseClicked(mouseEvent -> bufferAndQuery(mouseEvent));
            }
        } else {
            mapView.setOnMouseClicked(null);
            if (null != sceneView) {
                sceneView.setOnMouseClicked(null);
            }
        }
    }
    
    /**
     * Exercise 4: Convert a MouseEvent to a geographic point in the MapView or
     * SceneView's spatial reference.
     * @param event The MouseEvent.
     * @return A geographic point in the MapView or SceneView's spatial reference.
     */
    private Point getGeoPoint(MouseEvent event) {
        Point2D screenPoint = new Point2D(event.getX(), event.getY());
        Point geoPoint = threeD ?
                sceneView.screenToBaseSurface(screenPoint) :
                mapView.screenToLocation(screenPoint);
        return geoPoint;
    }

    /**
     * Exercise 4: Buffer and query
     */
    private void bufferAndQuery(MouseEvent event) {
        if (MouseButton.PRIMARY.equals(event.getButton()) && event.isStillSincePress()) {
            Point geoPoint = getGeoPoint(event);
            // Project to meters to do the buffer
            geoPoint = (Point) GeometryEngine.project(geoPoint, SpatialReference.create(3857));
            // Buffer by 1000 meters
            Polygon buffer = GeometryEngine.buffer(geoPoint, 1000.0);

            // Show click and buffer as graphics
            ListenableList<Graphic> graphics =
                    (threeD ? bufferAndQuerySceneGraphics : bufferAndQueryMapGraphics)
                            .getGraphics();
            graphics.clear();
            graphics.add(new Graphic(buffer, BUFFER_SYMBOL));
            graphics.add(new Graphic(geoPoint, CLICK_SYMBOL));

            // Run the query
            QueryParameters query = new QueryParameters();
            query.setGeometry(buffer);
            LayerList operationalLayers = threeD ?
                    sceneView.getArcGISScene().getOperationalLayers() :
                    mapView.getMap().getOperationalLayers();
            operationalLayers.parallelStream().filter(
                    layer -> layer instanceof FeatureLayer
            ).forEach(layer -> {
                /**
                 * Note: As of ArcGIS Runtime Quartz Beta 2, this select successfully
                 * selects features, but those features are only highlighted on the
                 * 2D MapView, not on the 3D SceneView. This behavior is scheduled
                 * to be fixed in ArcGIS Runtime Quartz.
                 */
                ((FeatureLayer) layer).selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
            });
        }
    };

    /**
     * Exercise 1: Main method that runs the app.
     * @param args Command line arguments (none are expected for this app).
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}