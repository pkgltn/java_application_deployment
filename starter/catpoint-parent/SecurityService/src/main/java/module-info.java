module SecurityService {
	requires ImageService;
	requires com.miglayout.swing;
	requires java.desktop;
	requires com.google.common;
	requires com.google.gson;
	requires java.prefs;
	requires java.sql;
	opens com.udacity.catpoint.data to com.google.gson;

}