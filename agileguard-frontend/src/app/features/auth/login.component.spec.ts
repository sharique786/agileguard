import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => expect(component).toBeTruthy());

  it('should disable submit when form is invalid', () => {
    component.form.patchValue({ email: '', password: '' });
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(btn.disabled).toBeTrue();
  });

  it('should call auth.login with form values on submit', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(of({ success: true, data: { accessToken: 't' } } as any));
    component.form.patchValue({ email: 'admin@db.com', password: 'Admin@1234' });
    component.onSubmit();
    tick();
    expect(authServiceSpy.login).toHaveBeenCalledWith({ email: 'admin@db.com', password: 'Admin@1234' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  }));

  it('should show error message on login failure', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ error: { message: 'Invalid credentials' } })));
    component.form.patchValue({ email: 'x@x.com', password: 'bad' });
    component.onSubmit();
    tick();
    expect(component.error()).toBe('Invalid credentials');
    expect(component.loading()).toBeFalse();
  }));

  it('should not call login when form is invalid', () => {
    component.form.patchValue({ email: '', password: '' });
    component.onSubmit();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });
});
