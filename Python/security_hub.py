#!/usr/bin/env python
from flask import Flask
from flask import request
from flask_httpauth import HTTPBasicAuth
import led
import requests
from led import red_on, blue_on, green_on, magenta_on, yellow_on, cyan_on, white_on
import zeroconf
import socket

app = Flask(__name__)

@app.route('/LED')
def handleLED():
    status = request.args.get('status')
    color = request.args.get('color')
    intensity = request.args.get('intensity')
    zc = zeroconf.Zeroconf()
    info = zc.get_service_info("_http._tcp.local.", "LED PI._http._tcp.local.")
    
    payload = {'color': color, 'status': status, 'intensity' : intensity}
    
    address = "http://" + socket.inet_ntoa(info.address) + ":" + str(info.port) + "/LED"
    zc = requests.get(address, params=payload)
    return "Color changed to: " + color

if __name__ == '__main__':
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = (s.getsockname()[0])
    s.close()
    app.run(host=ip, port=5000, debug=True)
    
