import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HistoryComponent, TransactionRecord } from './history';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let router: Router;

  const dummyTransactions: TransactionRecord[] = [
    {
      id: 'TX1',
      fromAccountNumber: 'APXAC00001',
      toAccountNumber: 'APXAC00002',
      amount: 500,
      status: 'SUCCESS',
      failureReason: null,
      idempotencyKey: 'key1',
      createdOn: '2026-06-23T12:00:00Z'
    },
    {
      id: 'TX2',
      fromAccountNumber: 'APXAC00002',
      toAccountNumber: 'APXAC00001',
      amount: 300,
      status: 'SUCCESS',
      failureReason: null,
      idempotencyKey: 'key2',
      createdOn: '2026-06-23T12:05:00Z'
    }
  ];

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['currentAccountNumber']);
    const accountSpy = jasmine.createSpyObj('AccountService', ['getTransactions', 'downloadStatementPdf']);

    await TestBed.configureTestingModule({
      imports: [HistoryComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: AccountService, useValue: accountSpy },
        provideRouter([])
      ]
    }).compileComponents();

    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  it('should redirect to /login if no current account number exists', () => {
    authServiceSpy.currentAccountNumber.and.returnValue(null);
    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });


  it('should load transactions successfully when component initializes', () => {
    authServiceSpy.currentAccountNumber.and.returnValue('APXAC00001');
    accountServiceSpy.getTransactions.and.returnValue(of(dummyTransactions));

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.currentAccountNumber()).toBe('APXAC00001');
    expect(component.transactions()).toEqual(dummyTransactions);
    expect(component.isLoading()).toBeFalse();
    expect(component.errorMessage()).toBeNull();
  });

  it('should show error message if transactions fetch fails', () => {
    authServiceSpy.currentAccountNumber.and.returnValue('APXAC00001');
    accountServiceSpy.getTransactions.and.returnValue(throwError(() => new Error('Fetch failed')));

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLoading()).toBeFalse();
    expect(component.errorMessage()).toBe('Failed to retrieve transaction logs. Please try again.');
  });

  it('should correctly identify debit vs credit transactions', () => {
    authServiceSpy.currentAccountNumber.and.returnValue('APXAC00001');
    accountServiceSpy.getTransactions.and.returnValue(of(dummyTransactions));

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // TX1 is from APXAC00001 -> debit
    expect(component.isDebit(dummyTransactions[0])).toBeTrue();
    // TX2 is from APXAC00002 -> credit
    expect(component.isDebit(dummyTransactions[1])).toBeFalse();
  });

  it('should download PDF statement successfully', () => {
    authServiceSpy.currentAccountNumber.and.returnValue('APXAC00001');
    accountServiceSpy.getTransactions.and.returnValue(of(dummyTransactions));
    const dummyBlob = new Blob(['pdf-content'], { type: 'application/pdf' });
    accountServiceSpy.downloadStatementPdf.and.returnValue(of(dummyBlob));

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Mock URL.createObjectURL and revokeObjectURL to avoid actual window interactions
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:url');
    spyOn(window.URL, 'revokeObjectURL');

    component.downloadPdf();

    expect(component.isDownloading()).toBeFalse();
    expect(accountServiceSpy.downloadStatementPdf).toHaveBeenCalledWith('APXAC00001');
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(dummyBlob);
  });

  it('should handle PDF download failure gracefully', () => {
    authServiceSpy.currentAccountNumber.and.returnValue('APXAC00001');
    accountServiceSpy.getTransactions.and.returnValue(of(dummyTransactions));
    accountServiceSpy.downloadStatementPdf.and.returnValue(throwError(() => new Error('Download failed')));

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.downloadPdf();

    expect(component.isDownloading()).toBeFalse();
    expect(component.errorMessage()).toBe('Failed to download statement. Please try again.');
  });
});
