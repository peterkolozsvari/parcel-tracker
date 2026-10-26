import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./components/home/home')
        .then(m => m.Home)
  },
  {
    path: 'navbar',
    loadComponent: () =>
      import('./components/navbar/navbar')
        .then(m => m.Navbar)
  },
  {
    path: 'shipments/new',
    loadComponent: () =>
      import('./components/create-shipment/create-shipment')
        .then(m => m.CreateShipment)
  },
  {
    path: 'shipments/success',
    loadComponent: () =>
      import('./components/shipment-successfully-created/shipment-successfully-created')
        .then(m => m.ShipmentSuccessfullyCreated)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login')
        .then(m => m.Login)
  },
  {
    path: 'page-not-found',
    loadComponent: () =>
      import('./components/page-not-found/page-not-found')
        .then(m => m.PageNotFound)
  },
  {
    path: '**',
    redirectTo: 'page-not-found'
  }
];

