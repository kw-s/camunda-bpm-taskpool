import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import 'rxjs/add/observable/of';
import { ProcesslistComponent } from './process-list.component';
import { Observable } from 'rxjs-compat';
import { FormsModule } from '@angular/forms';

import { FilterService } from 'app/services/filter.service';
import { ProcessHelperService } from 'app/services/process.helper.service';

describe('Component: TasklistComponent', () => {

  let component: ProcesslistComponent;
  let fixture: ComponentFixture<ProcesslistComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      imports: [
        FormsModule
      ],
      declarations: [
        ProcesslistComponent
      ],
      providers: [
        FilterService,
        { provide: ProcessHelperService, useValue: { processes: Observable.of([]) }}
      ],
    }).compileComponents().then(() => {
      // create component and test fixture
      fixture = TestBed.createComponent(ProcesslistComponent);

      // get test component from the fixture
      component = fixture.componentInstance;

      // detect changes
      fixture.detectChanges();
    });
  }));

  it('should create', () => {
    expect(component).toBeDefined();
  });

});
