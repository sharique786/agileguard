import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { StoryFormComponent } from './story-form.component';
import { AiService } from '../../core/services/ai.service';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

describe('StoryFormComponent', () => {
  let component: StoryFormComponent;
  let fixture: ComponentFixture<StoryFormComponent>;
  let aiSpy: jasmine.SpyObj<AiService>;

  beforeEach(async () => {
    aiSpy = jasmine.createSpyObj('AiService', ['validate', 'generateAC', 'streamValidation']);

    await TestBed.configureTestingModule({
      imports: [StoryFormComponent, ReactiveFormsModule],
      providers: [{ provide: AiService, useValue: aiSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(StoryFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => expect(component).toBeTruthy());

  it('should show all 5 checklist items', () => {
    expect(component.checklist().length).toBe(5);
  });

  it('should mark title checklist item done when title is filled', () => {
    component.form.patchValue({ title: 'Valid story title' });
    const titleItem = component.checklist().find(i => i.label.includes('title'));
    expect(titleItem?.done).toBeTrue();
  });

  it('should mark description checklist item done when >= 20 chars', () => {
    component.form.patchValue({ description: 'This is a valid description that is long enough.' });
    const descItem = component.checklist().find(i => i.label.includes('Description'));
    expect(descItem?.done).toBeTrue();
  });

  it('should call ai.generateAC when generateAC() called with title', fakeAsync(() => {
    aiSpy.generateAC.and.returnValue(of({ success: true, data: ['Given...Then...'] } as any));
    component.form.patchValue({ title: 'Test story', description: 'Description here' });
    component.generateAC();
    tick();
    expect(aiSpy.generateAC).toHaveBeenCalled();
  }));

  it('should patch AC field when AI generates criteria', fakeAsync(() => {
    aiSpy.generateAC.and.returnValue(of({ success: true, data: ['Given A When B Then C'] } as any));
    component.form.patchValue({ title: 'Test', description: 'Description test here' });
    component.generateAC();
    tick();
    expect(component.form.value.acceptanceCriteria).toContain('Given A When B Then C');
  }));

  it('should mark all touched fields on invalid submit', () => {
    component.form.patchValue({ title: '', description: '', acceptanceCriteria: '' });
    component.onSubmit();
    expect(component.form.controls['title'].touched).toBeTrue();
  });

  it('should return score class "low" for score < 50', () => {
    expect(component.getScoreClass(30)).toBe('low');
  });

  it('should return score class "high" for score >= 75', () => {
    expect(component.getScoreClass(80)).toBe('high');
  });

  afterEach(() => component.ngOnDestroy());
});
