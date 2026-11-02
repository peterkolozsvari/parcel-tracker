import {Routes} from '@angular/router';
import {roleGuard} from './guards/role.guard';
import {authGuard} from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'admin/users',
    canActivate: [roleGuard],
    data: {role: 'ADMIN'},
    loadComponent: () => import('./components/user-management/user-management').then(m => m.UserManagement)
  },
  {
    path: 'admin/shipments',
    canActivate: [roleGuard],
    data: {role: 'ADMIN'},
    loadComponent: () => import('./components/shipment-management/shipment-management').then(m => m.ShipmentManagement)
  },
  {
    path: 'courier/shipments',
    canActivate: [roleGuard],
    data: {role: 'COURIER'},
    loadComponent: () => import('./components/shipment-management/shipment-management').then(m => m.ShipmentManagement)
  },
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
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/create-shipment/create-shipment')
        .then(m => m.CreateShipment)
  },
  {
    path: 'shipments/success',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/shipment-successfully-created/shipment-successfully-created')
        .then(m => m.ShipmentSuccessfullyCreated)
  },
  {
    path: 'shipments/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/shipment-details/shipment-details')
        .then(m => m.ShipmentDetails)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login')
        .then(m => m.Login)
  },
  {
    path: 'registration',
    loadComponent: () =>
      import('./components/registration/registration')
        .then(m => m.Registration)
  },
  {
    path: 'my-shipments',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/my-shipments/my-shipments')
        .then(m => m.MyShipments)
  },
  {
    path: 'my-profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/my-profile/my-profile')
        .then(m => m.MyProfile)
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


