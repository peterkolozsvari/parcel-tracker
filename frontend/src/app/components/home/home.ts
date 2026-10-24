import {Component, inject, OnInit} from '@angular/core';
import {UserService} from '../../services/user.service';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {

  private readonly userService = inject(UserService);


  ngOnInit(): void {
    this.userService.getUserById(1).subscribe({
      next: user => console.log('Backend válasza:', user),
      error: error => console.error('Backendhívás hibája:', error)
    });
  }

}
