import Foundation
import Capacitor
import UserNotifications
import CoreLocation

@objc(CapacitorBackgroundLocationPlugin)
public class CapacitorBackgroundLocationPlugin: CAPPlugin, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    private let EVENT_NAME: String = "CHANGE";
    private let ERROR_EVENT_NAME: String = "ERROR";
    private final let notificationCenter = UNUserNotificationCenter.current();
    
    private var title: String = "";
    private var desc: String = "";
    private var urlPath: String? = nil;
    private var headers: JSObject = [:];
    private var body: JSObject = [:];
    
    private var locationManager: CLLocationManager? = nil;
    
    @objc public override func load() {
        self.locationManager = CLLocationManager();
        self.locationManager?.delegate = self;
        self.locationManager?.allowsBackgroundLocationUpdates = true;
        self.locationManager?.showsBackgroundLocationIndicator  = true;
        self.locationManager?.pausesLocationUpdatesAutomatically = false;
        self.locationManager?.requestAlwaysAuthorization();
        
        self.notificationCenter.delegate = self;
        self.notificationCenter.requestAuthorization(options: [.alert, .badge, .sound]) { (granted, error) in
            if granted {
                print("Yay!")
            } else {
                print("D'oh")
                self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : "NOTIFICATION_UNPERMITTED"]);
            }
        };
    }
    
    @objc func showNotification(title: String, description: String) {
        self.notificationCenter.delegate = self;
        
        self.notificationCenter.removeAllDeliveredNotifications();
        
        let content = UNMutableNotificationContent();
        content.title = title;
        content.subtitle = "";
        content.body = description;
        content.categoryIdentifier = "bg-location-ntf";
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 7.0, repeats: false);
        
        let request = UNNotificationRequest(identifier: "bg-location-ntf", content: content, trigger: trigger );
        
        self.notificationCenter.add(request, withCompletionHandler: nil);
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        print("locationManagerDidChangeAuthorization----->")
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let _latitude = self.locationManager?.location?.coordinate.latitude;
        let _longitude = self.locationManager?.location?.coordinate.longitude;
        let _accuracy = self.locationManager?.location?.horizontalAccuracy;
        let _altitude = self.locationManager?.location?.altitude;
        let _bearing  = self.locationManager?.location?.course;
        let _speed = self.locationManager?.location?.speed;
        let _locTime = self.locationManager?.location?.timestamp;
        
        let outDateFormatter: DateFormatter = {
            let df = DateFormatter();
            df.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
            df.locale = Locale(identifier: "en_US_POSIX");
            return df;
        }()
        
        let _time = outDateFormatter.string(from: _locTime!);
        
        let _data: [String : Any] = [
            "latitude": _latitude,
            "longitude": _longitude,
            "accuracy": _accuracy,
            "altitude": _altitude,
            "bearing": _bearing,
            "speed": _speed,
            "time": _time,
            "lastUpdate": _time
        ];
        
        
        self.notifyListeners(self.EVENT_NAME, data: _data);
        
        self.sendData(_data: _data);
    }
    
    @objc func sendData(_data: [String : Any] ) -> Void {
        do {
            if self.urlPath == nil {
                return;
            }
            
            
            let url = URL(string: self.urlPath ?? "");
            guard let requestUrl = url else { return; }
            
            var request = URLRequest(url: requestUrl);
            request.httpMethod = "POST";
            
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            for hk in self.headers.keys {
                let _val = self.headers[hk];
                request.setValue(_val as? String, forHTTPHeaderField: hk);
            }
            
            var body:JSObject = [:];
            
            for key in self.body.keys {
                body[key] = self.body[key]
            }
            
            for (k, v) in _data {
                body[k] = v as! JSValue;
            }
            
            do {
                request.httpBody = try JSONSerialization.data(withJSONObject: body);
            } catch {
                self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : "SERILIZATION_PROBLEM"]);
                return;
            }
            
            
            URLSession.shared.dataTask(with: request) { (data, response, error) in
                
                let _res = response as? HTTPURLResponse;
                
                print("RESPONSE_CODE", _res?.statusCode)
                
                if _res?.statusCode == 200 || _res?.statusCode == 201 {
                    print("Response data string:\n")
                    
                }else{
                    print("Error took place");
                    self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : response]);
                    return;
                }
            }.resume()
        } catch {
            self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : "DATA_SENDING_ERROR"]);
        }
    }
    
    
    @objc func setConfig(_ call: CAPPluginCall){
        self.title = call.getString("title", "Location service is running");
        self.desc = call.getString("description", "Location service is running");
        self.urlPath = call.getString("url");
        self.headers = call.getObject("headers", [:]);
        self.body = call.getObject("body", [:]);
    }
    
    @objc func start(_ call: CAPPluginCall) {
        self.locationManager?.startUpdatingLocation();
        
        self.showNotification(title: self.title, description: self.desc);
        
        call.resolve([:]);
    }
    
    @objc func stop(_ call: CAPPluginCall) {
        self.locationManager?.stopUpdatingLocation();
        
        call.resolve([:]);
    }
}
