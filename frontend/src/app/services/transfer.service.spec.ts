import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TransferService, TransferRequestData } from './transfer.service';

describe('TransferService', () => {
  let service: TransferService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransferService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TransferService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should execute a transfer', () => {
    const requestData: TransferRequestData = {
      fromAccountNumber: 'APXAC1',
      toAccountNumber: 'APXAC2',
      amount: 100,
      idempotencyKey: 'key'
    };
    const dummyResponse = { status: 'SUCCESS', transactionId: 'TX1' };

    service.transfer(requestData).subscribe((res) => {
      expect(res).toEqual(dummyResponse);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/transfers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(requestData);
    req.flush(dummyResponse);
  });
});
