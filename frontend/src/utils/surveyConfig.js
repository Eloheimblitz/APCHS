export const optionSets = {
  yesNo: [
    { value: true, label: 'Yes' },
    { value: false, label: 'No' }
  ],
  studyArea: ['BYRNIHAT', 'NONGPOH'],
  gender: ['MALE', 'FEMALE'],
  tobaccoUse: ['SMOKING', 'EATING', 'SECONDHAND', 'NONE'],
  ethnicity: ['KHASI', 'GARO', 'ASSAMESE', 'OTHER'],
  education: ['BELOW_10', 'CLASS_10', 'CLASS_12', 'UG', 'PG', 'OTHER'],
  occupation: ['FARMER', 'LABOURER', 'FACTORY', 'STUDENT', 'BUSINESS', 'HOME', 'OTHER'],
  cookingFuel: ['GAS', 'WOOD', 'COAL', 'ELECTRICITY'],
  woodCoalCookingLocation: ['IN', 'OUT', 'NA'],
  childBirthplace: ['HOME', 'HOSPITAL', 'NA'],
  childVaccination: ['YES', 'NO', 'NA'],
  feverDuration: ['LESS_THAN_5_DAYS', 'MORE_THAN_5_DAYS', 'NA']
};

export const SYMPTOM_CATALOG = [
  { key: 'HEADACHE', label: 'Headache' },
  { key: 'EYE_IRRITATION', label: 'Eye Irritation' },
  { key: 'RHINITIS', label: 'Rhinitis' },
  { key: 'SNEEZING', label: 'Sneezing' },
  { key: 'SINUSITIS', label: 'Sinusitis' },
  { key: 'SORE_THROAT', label: 'Sore Throat' },
  { key: 'COLD', label: 'Cold' },
  { key: 'FEVER', label: 'Fever' },
  { key: 'DRY_COUGH', label: 'Dry Cough' },
  { key: 'WET_COUGH', label: 'Wet Cough' },
  { key: 'WHEEZING', label: 'Wheezing' },
  { key: 'BREATHLESSNESS', label: 'Breathlessness' },
  { key: 'CHEST_DISCOMFORT', label: 'Chest Discomfort' },
  { key: 'SLEEP_DISTURBANCE', label: 'Sleep Disturbance' },
  { key: 'SKIN_IRRITATION', label: 'Skin Irritation' }
];

export const CONDITION_CATALOG = [
  { key: 'ASTHMA', label: 'Asthma' },
  { key: 'INHALER_USE', label: 'Inhaler Use' },
  { key: 'TUBERCULOSIS', label: 'Tuberculosis' },
  { key: 'HEART_PROBLEMS', label: 'Heart Problems' },
  { key: 'DIABETES', label: 'Diabetes' },
  { key: 'HIGH_BP', label: 'High BP' },
  { key: 'CANCER', label: 'Cancer' }
];

function emptyHealthItem(key) {
  return {
    key,
    present: false,
    visitedHospital: false,
    hospitalNames: '',
    ipd: false,
    opd: false,
    missedSchoolOrWork: false,
    daysMissed: ''
  };
}

function emptyOtherIssue() {
  return {
    description: '',
    visitedHospital: false,
    hospitalNames: '',
    ipd: false,
    opd: false,
    missedSchoolOrWork: false,
    daysMissed: ''
  };
}

export const defaultSurvey = {
  surveyDate: new Date().toISOString().slice(0, 10),
  consentObtained: true,
  studyArea: 'BYRNIHAT',
  gender: 'MALE',
  age: '',
  symptoms: SYMPTOM_CATALOG.map((item) => emptyHealthItem(item.key)),
  conditions: CONDITION_CATALOG.map((item) => emptyHealthItem(item.key)),
  otherIssues: Array.from({ length: 5 }, emptyOtherIssue),
  tobaccoUse: [],
  occupation: [],
  primaryCookingFuel: [],
  woodCoalCookingLocation: [],
  childBirthplace: []
};

export const sections = [
  {
    title: 'A. Survey Information',
    fields: [
      { name: 'surveyId', label: 'Survey ID (optional - leave blank to auto-generate)' },
      { name: 'surveyDate', label: 'Survey date', type: 'date', required: true },
      { name: 'surveyorId', label: 'Surveyor name', required: true },
      { name: 'consentObtained', label: 'Consent obtained', type: 'boolean', required: true },
      { name: 'studyArea', label: 'Study area', type: 'select', options: optionSets.studyArea, required: true },
      { name: 'gridId', label: 'Grid ID' },
      { name: 'latitude', label: 'Latitude', type: 'number', step: 'any' },
      { name: 'longitude', label: 'Longitude', type: 'number', step: 'any' },
      { name: 'gpsAccuracy', label: 'GPS accuracy in meters', type: 'number', step: 'any' },
      { name: 'distanceToHighway', label: 'Distance to highway' },
      { name: 'distanceToFactory', label: 'Distance to factory' }
    ]
  },
  {
    title: 'B. Demographics',
    fields: [
      { name: 'age', label: 'Age', type: 'number', required: true },
      { name: 'durationOfStayAtStudyArea', label: 'Duration of stay at study area' },
      { name: 'gender', label: 'Gender', type: 'select', options: optionSets.gender, required: true },
      { name: 'tobaccoUse', label: 'Tobacco', type: 'multiselect', options: optionSets.tobaccoUse },
      { name: 'alcohol', label: 'Alcohol', type: 'boolean' },
      { name: 'ethnicity', label: 'Ethnicity', type: 'select', options: optionSets.ethnicity },
      { name: 'otherEthnicity', label: 'Other ethnicity', required: true, showWhen: { ethnicity: 'OTHER' } },
      { name: 'education', label: 'Education', type: 'select', options: optionSets.education },
      { name: 'otherEducation', label: 'Other education', required: true, showWhen: { education: 'OTHER' } },
      { name: 'occupation', label: 'Occupation', type: 'multiselect', options: optionSets.occupation },
      { name: 'otherOccupation', label: 'Other occupation', required: true, showWhen: { occupation: 'OTHER' } }
    ]
  },
  {
    title: 'C. Cooking',
    fields: [
      { name: 'primaryCookingFuel', label: 'Cooking', type: 'multiselect', options: optionSets.cookingFuel, required: true },
      { name: 'woodCoalCookingLocation', label: 'Wood/coal cooking location', type: 'multiselect', options: optionSets.woodCoalCookingLocation }
    ]
  },
  {
    title: 'D. Children and Vaccination',
    fields: [
      { name: 'hasChildren', label: 'Do you have children?', type: 'boolean' },
      { name: 'numberOfChildren', label: 'Number of children', type: 'number', showWhen: { hasChildren: true } },
      { name: 'childBirthplace', label: 'Child birthplace', type: 'multiselect', options: optionSets.childBirthplace },
      { name: 'childVaccination', label: 'Child vaccination', type: 'select', options: optionSets.childVaccination },
      { name: 'respondentVaccination', label: 'Respondent vaccination', type: 'boolean' },
      { name: 'mhisSmartCard', label: 'MHIS/smart card', type: 'boolean' }
    ]
  },
  {
    title: 'E. Existing Health Conditions',
    type: 'healthTable',
    catalogKey: 'conditions',
    catalog: CONDITION_CATALOG,
    fields: [
      { name: 'cancerType', label: 'Type of Cancer' }
    ]
  },
  {
    title: 'F. Symptoms (last 2 months)',
    type: 'healthTable',
    catalogKey: 'symptoms',
    catalog: SYMPTOM_CATALOG,
    fields: [
      { name: 'feverDuration', label: 'Fever duration', type: 'select', options: optionSets.feverDuration }
    ]
  },
  {
    title: 'G. Other Issues',
    type: 'healthTable',
    catalogKey: 'otherIssues',
    catalog: null,
    fields: [
      { name: 'remarks', label: 'Remarks', type: 'textarea' }
    ]
  }
];

export function labelize(value) {
  return String(value)
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/\bBp\b/g, 'BP');
}
