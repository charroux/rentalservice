import {Routes} from '@angular/router';
import {CarsListComponent} from './cars-list/cars-list.component';
import {CarRentalComponent} from './car-rental/car-rental.component';
import {ValidateRentalComponent} from './validate-rental/validate-rental.component';

const routeConfig: Routes = [
    {
      path: '',
      component: CarsListComponent,
      title: 'Cars List',
    },
    {
      path: 'details/:id',
      component: CarRentalComponent,
      title: 'Car Rental Details',
    },
    {
      path: 'validate/:id/:plateNumber',
      component: ValidateRentalComponent,
      title: 'Validate Rental',
    },
  ];
  export default routeConfig;

