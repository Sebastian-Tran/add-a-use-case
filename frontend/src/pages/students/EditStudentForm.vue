<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <!-- Offered only while she is active (UC-STU-deactivate-student 3a) -->
      <el-button v-if="studentBeingEdited.enabled" type="danger" plain @click="deactivate()">
        Deactivate
      </el-button>
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog', 'student-deactivated'])

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function deactivate() {
  const name = `${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}`
  try {
    await ElMessageBox.confirm(DEACTIVATION_WARNING, `Deactivate ${name}?`, {
      confirmButtonText: 'Deactivate',
      cancelButtonText: 'Cancel',
      type: 'warning',
      dangerouslyUseHTMLString: true // A constant, no user input
    })
  } catch {
    return // Cancelled: no request, the details stay open (5a)
  }

  try {
    const result = await deactivateStudent(studentBeingEdited.value.id as number)
    studentBeingEdited.value.enabled = result.data.enabled
    ElMessage.success(`${name} has been deactivated.`)
    emit('student-deactivated')
  } catch {
    // Already deactivated elsewhere (6a): the shared request instance has shown the message. Reload so the
    // details stop offering Deactivate.
    await loadStudent(studentBeingEdited.value.id as number)
  }
}

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}
</script>

<script lang="ts">
// The consequences shown before confirming (UC-STU-deactivate-student step 4, BR-student-lifecycle)
const DEACTIVATION_WARNING = `
  <p>Deactivating this student means:</p>
  <ul>
    <li>She can no longer log in or submit weekly activity reports and peer evaluations, effective immediately.</li>
    <li>She stays on her team, and her previously submitted work is kept.</li>
    <li>Her teammates may still evaluate her for last week until its submission window closes, but not for this week or later.</li>
    <li>The course section's peer evaluation report shows her only for the weeks before this week.</li>
    <li>She no longer receives submission reminders.</li>
  </ul>
  <p>She can be reactivated later. This is not deletion.</p>`
</script>

<style scoped lang="scss"></style>
