import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { SessionService } from '../../common/session.service';
import { AuthService } from '../../common/auth.service';
import { ConsentSharingComponent } from '../entry-page/initiation/consent-sharing/consent-sharing.component';

@Component({
  selector: 'consent-app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  public static ROUTE = 'login';

  loginForm: FormGroup;
  private route: ActivatedRouteSnapshot;
  private redirectCode: string;
  private authId: string;

  constructor(
    private formBuilder: FormBuilder,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private sessionService: SessionService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      login: ['', Validators.required],
      password: ['', Validators.required]
    });

    this.route = this.activatedRoute.snapshot;
    this.authId = this.route.parent.params.authId;
    this.redirectCode = this.route.queryParams.redirectCode;
    if (this.redirectCode) {
      this.sessionService.setRedirectCode(this.authId, this.redirectCode);
    }
  }

  onSubmit() {
    this.authService.userLogin(this.loginForm.value).subscribe(
      res => {
        // store xsrf-token in session-storage
        const xsrfToken = res.body.xsrfToken;
        this.sessionService.setXsrfToken(xsrfToken);
        // navigate to transactions
        this.router.navigate(['../' + ConsentSharingComponent.ROUTE], { relativeTo: this.activatedRoute });
      },
      error => {
        console.log(error);
      }
    );
  }

  get login() {
    return this.loginForm.get('login');
  }
  get password() {
    return this.loginForm.get('password');
  }
}
