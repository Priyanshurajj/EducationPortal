package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.model.Classroom
import com.example.educationportal.data.model.ClassroomDetail
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.repository.ClassroomRepository
import com.example.educationportal.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ClassroomListState(
    val classrooms: List<Classroom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CreateClassroomState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val createdClassroom: Classroom? = null,
    val errorMessage: String? = null
)

data class EnrollClassroomState(
    val classCode: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val enrolledClassroom: Classroom? = null,
    val errorMessage: String? = null
)

data class ClassroomDetailState(
    val classroom: ClassroomDetail? = null,
    val materials: List<Material> = emptyList(),
    val isLoading: Boolean = false,
    val isMaterialsLoading: Boolean = false,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed class ClassroomEvent {
    data object LoadClassrooms : ClassroomEvent()
    data object RefreshClassrooms : ClassroomEvent()
    
    // Create classroom events
    data class CreateNameChanged(val name: String) : ClassroomEvent()
    data class CreateDescriptionChanged(val description: String) : ClassroomEvent()
    data object CreateClassroom : ClassroomEvent()
    data object ResetCreateState : ClassroomEvent()
    
    // Enroll events
    data class EnrollCodeChanged(val code: String) : ClassroomEvent()
    data object EnrollInClassroom : ClassroomEvent()
    data object ResetEnrollState : ClassroomEvent()
    
    // Detail events
    data class LoadClassroomDetail(val classroomId: Int) : ClassroomEvent()
    data class LoadMaterials(val classroomId: Int) : ClassroomEvent()
    data class UploadMaterial(
        val classroomId: Int,
        val title: String,
        val description: String?,
        val file: File
    ) : ClassroomEvent()
    data class DeleteMaterial(val classroomId: Int, val materialId: Int) : ClassroomEvent()
    data class DeleteClassroom(val classroomId: Int) : ClassroomEvent()
    data class UnenrollFromClassroom(val classroomId: Int) : ClassroomEvent()
    data object ResetUploadSuccess : ClassroomEvent()
    
    data object ClearError : ClassroomEvent()
}

class ClassroomViewModel(
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(ClassroomListState())
    val listState: StateFlow<ClassroomListState> = _listState.asStateFlow()

    private val _createState = MutableStateFlow(CreateClassroomState())
    val createState: StateFlow<CreateClassroomState> = _createState.asStateFlow()

    private val _enrollState = MutableStateFlow(EnrollClassroomState())
    val enrollState: StateFlow<EnrollClassroomState> = _enrollState.asStateFlow()

    private val _detailState = MutableStateFlow(ClassroomDetailState())
    val detailState: StateFlow<ClassroomDetailState> = _detailState.asStateFlow()

    init {
        loadClassrooms()
    }

    fun onEvent(event: ClassroomEvent) {
        when (event) {
            is ClassroomEvent.LoadClassrooms -> loadClassrooms()
            is ClassroomEvent.RefreshClassrooms -> loadClassrooms()
            
            is ClassroomEvent.CreateNameChanged -> {
                _createState.update { it.copy(name = event.name) }
            }
            is ClassroomEvent.CreateDescriptionChanged -> {
                _createState.update { it.copy(description = event.description) }
            }
            is ClassroomEvent.CreateClassroom -> createClassroom()
            is ClassroomEvent.ResetCreateState -> {
                _createState.value = CreateClassroomState()
            }
            
            is ClassroomEvent.EnrollCodeChanged -> {
                _enrollState.update { it.copy(classCode = event.code) }
            }
            is ClassroomEvent.EnrollInClassroom -> enrollInClassroom()
            is ClassroomEvent.ResetEnrollState -> {
                _enrollState.value = EnrollClassroomState()
            }
            
            is ClassroomEvent.LoadClassroomDetail -> loadClassroomDetail(event.classroomId)
            is ClassroomEvent.LoadMaterials -> loadMaterials(event.classroomId)
            is ClassroomEvent.UploadMaterial -> uploadMaterial(
                event.classroomId,
                event.title,
                event.description,
                event.file
            )
            is ClassroomEvent.DeleteMaterial -> deleteMaterial(event.classroomId, event.materialId)
            is ClassroomEvent.DeleteClassroom -> deleteClassroom(event.classroomId)
            is ClassroomEvent.UnenrollFromClassroom -> unenrollFromClassroom(event.classroomId)
            is ClassroomEvent.ResetUploadSuccess -> {
                _detailState.update { it.copy(uploadSuccess = false) }
            }
            
            is ClassroomEvent.ClearError -> {
                _listState.update { it.copy(errorMessage = null) }
                _createState.update { it.copy(errorMessage = null) }
                _enrollState.update { it.copy(errorMessage = null) }
                _detailState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadClassrooms() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = classroomRepository.getMyClassrooms()) {
                is Resource.Success -> {
                    _listState.update {
                        it.copy(
                            classrooms = result.data ?: emptyList(),
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _listState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun createClassroom() {
        val currentState = _createState.value
        
        if (currentState.name.isBlank()) {
            _createState.update { it.copy(errorMessage = "Class name is required") }
            return
        }
        
        if (currentState.isLoading) return

        viewModelScope.launch {
            _createState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = classroomRepository.createClassroom(
                currentState.name.trim(),
                currentState.description.trim().ifBlank { null }
            )) {
                is Resource.Success -> {
                    _createState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdClassroom = result.data
                        )
                    }
                    // Refresh classrooms list
                    loadClassrooms()
                }
                is Resource.Error -> {
                    _createState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun enrollInClassroom() {
        val currentState = _enrollState.value
        
        if (currentState.classCode.isBlank()) {
            _enrollState.update { it.copy(errorMessage = "Class code is required") }
            return
        }
        
        if (currentState.classCode.length < 6) {
            _enrollState.update { it.copy(errorMessage = "Invalid class code") }
            return
        }
        
        if (currentState.isLoading) return

        viewModelScope.launch {
            _enrollState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = classroomRepository.enrollInClassroom(currentState.classCode.trim())) {
                is Resource.Success -> {
                    _enrollState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            enrolledClassroom = result.data
                        )
                    }
                    // Refresh classrooms list
                    loadClassrooms()
                }
                is Resource.Error -> {
                    _enrollState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadClassroomDetail(classroomId: Int) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, errorMessage = null) }
            
            when (val result = classroomRepository.getClassroomDetail(classroomId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(classroom = result.data, isLoading = false)
                    }
                    // Also load materials
                    loadMaterials(classroomId)
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadMaterials(classroomId: Int) {
        viewModelScope.launch {
            _detailState.update { it.copy(isMaterialsLoading = true) }
            
            when (val result = classroomRepository.getClassroomMaterials(classroomId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            materials = result.data ?: emptyList(),
                            isMaterialsLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isMaterialsLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun uploadMaterial(
        classroomId: Int,
        title: String,
        description: String?,
        file: File
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isUploading = true, errorMessage = null) }
            
            when (val result = classroomRepository.uploadMaterial(
                classroomId,
                title,
                description,
                file
            )) {
                is Resource.Success -> {
                    _detailState.update { it.copy(isUploading = false, uploadSuccess = true) }
                    // Refresh materials
                    loadMaterials(classroomId)
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isUploading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun deleteMaterial(classroomId: Int, materialId: Int) {
        viewModelScope.launch {
            when (val result = classroomRepository.deleteMaterial(classroomId, materialId)) {
                is Resource.Success -> {
                    // Refresh materials
                    loadMaterials(classroomId)
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun deleteClassroom(classroomId: Int) {
        viewModelScope.launch {
            when (val result = classroomRepository.deleteClassroom(classroomId)) {
                is Resource.Success -> {
                    loadClassrooms()
                }
                is Resource.Error -> {
                    _listState.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun unenrollFromClassroom(classroomId: Int) {
        viewModelScope.launch {
            when (val result = classroomRepository.unenrollFromClassroom(classroomId)) {
                is Resource.Success -> {
                    loadClassrooms()
                }
                is Resource.Error -> {
                    _listState.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
