import Foundation
import Capacitor
import UserNotifications
import CoreLocation

@objc(CapacitorBackgroundLocationPlugin)
public class CapacitorBackgroundLocationPlugin: CAPPlugin, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    private let EVENT_NAME: String = "CHANGE";
    private let ERROR_EVENT_NAME: String = "ERROR";
    private final let notificationCenter = UNUserNotificationCenter.current();
    
    private var interval: Float = 10;
    private var title: String = "";
    private var desc: String = "";
    private var urlPath: String? = nil;
    private var headers: JSObject = [:];
    private var body: JSObject = [:];
    
    private var lastUpdateTime: Date? = nil;
    
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
    
    func checkIsValidUpdate(_time: Date) -> Bool {
        if self.lastUpdateTime == nil {
            self.lastUpdateTime = Date();
            return true;
        }
        
        let formatter = DateComponentsFormatter()
        let diff = formatter.string(from: self.lastUpdateTime!, to: _time);
        let ch = Float(diff ?? "0")! > Float(self.interval);
        
        if Bool(ch){
            self.lastUpdateTime = _time;
        }
        return Bool(ch);
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        print("locationManagerDidChangeAuthorization----->")
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : "LOCATION_RECEIVE_ERROR"]);
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let _latitude = self.locationManager?.location?.coordinate.latitude ?? 0.0;
        let _longitude = self.locationManager?.location?.coordinate.longitude ?? 0.0;
        let _accuracy = self.locationManager?.location?.horizontalAccuracy ?? 0.0;
        let _altitude = self.locationManager?.location?.altitude ?? 0.0;
        let _bearing  = self.locationManager?.location?.course ?? 0.0;
        let _speed = self.locationManager?.location?.speed ?? 0.0;
        let _locApiTime = self.locationManager?.location?.timestamp ?? Date();
        
        if !self.checkIsValidUpdate(_time: Date()) {
            print("OPSSS INVALIDATE INTERVAL");
            return;
        }
        
        let outDateFormatter: DateFormatter = {
            let df = DateFormatter();
            df.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
            df.timeZone = TimeZone(secondsFromGMT: 0);
            return df;
        }()
        
        let _time = outDateFormatter.string(from: _locApiTime);
        
        let _data: [String : Any] = [
            "latitude": Float(_latitude),
            "longitude": Float(_longitude),
            "accuracy": Float(_accuracy),
            "altitude": Float(_altitude),
            "bearing": Float(_bearing),
            "angle": Float(_bearing),
            "speed": Float(_speed),
            "time": _time,
            "lastUpdate": _time
        ];
        
        self.notifyListeners(self.EVENT_NAME, data: _data);
        
        DispatchQueue.global().async {
            self.sendData(_data: _data);
        }
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
            
            var _body: [String : Any] = [:];
            
            for key in self.body.keys {
                _body[key] = self.body[key]
            }
            
            for (k, v) in _data {
                _body[k] = v;
            }
            
            do {
                request.httpBody = try JSONSerialization.data(withJSONObject: _body);
            } catch {
                self.notifyListeners(self.ERROR_EVENT_NAME, data: ["error" : "SERILIZATION_PROBLEM"]);
                return;
            }
            
            
            URLSession.shared.dataTask(with: request) { (data, response, error) in
                
                let _res = response as? HTTPURLResponse;
                
                // print("RESPONSE_CODE", _res?.statusCode)
                
                if _res?.statusCode == 200 || _res?.statusCode == 201 {
                    // print("Response data string:\n")
                    
                }else{
                    // print("Error took place");
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
        do{
            if CLLocationManager.locationServicesEnabled() == false {
                call.reject("GPS_IS_NOT_ENABLE");
                return;
            }
            
            let interval = call.getInt("interval", 15000);
            self.interval = Float(interval) / Float(1000);
            
            self.locationManager?.stopUpdatingLocation();
            self.locationManager?.requestLocation();
            self.locationManager?.startUpdatingLocation();
            
            self.showNotification(title: self.title, description: self.desc);
            
            call.resolve([:]);
        }catch{
            call.reject("START_ERROR");
        }
    }
    
    @objc func stop(_ call: CAPPluginCall) {
        do{
            self.locationManager?.stopUpdatingLocation();
            
            call.resolve([:]);
        }catch{
            call.reject("STOP_ERROR");
        }
    }
}
