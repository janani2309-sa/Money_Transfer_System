import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AccountService } from './account.service';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should download statement pdf as blob', () => {
    const dummyBlob = new Blob(['pdf-content'], { type: 'application/pdf' });
    const accountNumber = 'APXAC00001';

    service.downloadStatementPdf(accountNumber).subscribe((blob) => {
      expect(blob).toBeTruthy();
      expect(blob.type).toBe('application/pdf');
    });

    const req = httpMock.expectOne(`http://localhost:8080/api/v1/accounts/${accountNumber}/statement/pdf`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyBlob);
  });
});
