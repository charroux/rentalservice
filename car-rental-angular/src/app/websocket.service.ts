import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS  from 'sockjs-client';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
 // private client: Client;
  private plateNumberUpdates = new BehaviorSubject<any>(null);

  constructor() {
  /*  this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        console.log('Connected to WebSocket');
        this.subscribeToPlateNumbers();
      },
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      }
    });*/
  }

  connect(): void {
  //  this.client.activate();
  }

  disconnect(): void {
  //  this.client.deactivate();
  }

  private subscribeToPlateNumbers(): void {
 /*   this.client.subscribe('/topic/cars', (message) => {
      const carUpdate = JSON.parse(message.body);
      this.plateNumberUpdates.next(carUpdate);
    });*/
  }

  getPlateNumberUpdates() {
    return this.plateNumberUpdates.asObservable();
  }
}